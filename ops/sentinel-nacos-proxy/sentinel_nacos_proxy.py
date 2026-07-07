import json
import os
import time
import urllib.parse
import urllib.request
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


DASHBOARD_BASE_URL = os.getenv("DASHBOARD_BASE_URL", "http://sentinel:8080").rstrip("/")
DASHBOARD_USERNAME = os.getenv("DASHBOARD_USERNAME", "sentinel")
DASHBOARD_PASSWORD = os.getenv("DASHBOARD_PASSWORD", "sentinel")
NACOS_BASE_URL = os.getenv("NACOS_BASE_URL", "http://nacos:8848/nacos").rstrip("/")
NACOS_USERNAME = os.getenv("NACOS_USERNAME", "nacos")
NACOS_PASSWORD = os.getenv("NACOS_PASSWORD", "nacos")
NACOS_NAMESPACE = os.getenv("NACOS_NAMESPACE", "dev")
NACOS_GROUP = os.getenv("NACOS_GROUP", "XUAN_ERP_GROUP")
DATA_ID_PATTERN = os.getenv(
    "DATA_ID_PATTERN",
    "{app}-sentinel-{rule_type}-rules.json",
)
SYNC_APPS = [item.strip() for item in os.getenv("SYNC_APPS", "").split(",") if item.strip()]
SYNC_INTERVAL_SECONDS = int(os.getenv("SYNC_INTERVAL_SECONDS", "30"))

HOP_BY_HOP_HEADERS = {
    "connection",
    "content-length",
    "host",
    "keep-alive",
    "proxy-authenticate",
    "proxy-authorization",
    "te",
    "trailers",
    "transfer-encoding",
    "upgrade",
}

RULE_ENDPOINTS = {
    "flow": "/v2/flow/rules",
    "degrade": "/degrade/rules.json",
    "param-flow": "/paramFlow/rules",
}

RULE_MUTATION_PREFIXES = (
    "/v2/flow/rule",
    "/degrade/rule",
    "/paramFlow/rule",
)

RULE_MUTATION_PATHS = {
    "flow": "/v2/flow/rule",
    "degrade": "/degrade/rule",
    "param-flow": "/paramFlow/rule",
}

RULE_FIELDS = {
    "flow": [
        "resource",
        "limitApp",
        "grade",
        "count",
        "strategy",
        "refResource",
        "controlBehavior",
        "warmUpPeriodSec",
        "maxQueueingTimeMs",
        "clusterMode",
        "clusterConfig",
    ],
    "degrade": [
        "resource",
        "limitApp",
        "grade",
        "count",
        "timeWindow",
        "minRequestAmount",
        "statIntervalMs",
        "slowRatioThreshold",
    ],
    "param-flow": [
        "resource",
        "limitApp",
        "grade",
        "paramIdx",
        "count",
        "durationInSec",
        "controlBehavior",
        "maxQueueingTimeMs",
        "burstCount",
        "paramFlowItemList",
    ],
}


class NoDashboardMachine(RuntimeError):
    pass


def build_data_id(pattern, app, rule_type):
    return pattern.format(app=app, rule_type=rule_type)


def sanitize_rule(rule_type, rule):
    if rule_type == "param-flow" and isinstance(rule.get("rule"), dict):
        rule = rule["rule"]
    fields = RULE_FIELDS[rule_type]
    return {key: rule[key] for key in fields if key in rule and rule[key] is not None}


def select_machine(machines):
    if not machines:
        raise NoDashboardMachine("Dashboard has no machine for app")
    for machine in machines:
        if machine.get("healthy"):
            return machine
    return machines[0]


def http_request(method, url, headers=None, body=None, timeout=15):
    request = urllib.request.Request(url, data=body, method=method)
    for key, value in (headers or {}).items():
        request.add_header(key, value)
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return response.status, dict(response.headers.items()), response.read()
    except urllib.error.HTTPError as error:
        return error.code, dict(error.headers.items()), error.read()


class DashboardSession:
    def __init__(self):
        self.cookie = None
        self.login_at = 0

    def ensure_login(self):
        if self.cookie and time.time() - self.login_at < 3600:
            return
        params = urllib.parse.urlencode(
            {"username": DASHBOARD_USERNAME, "password": DASHBOARD_PASSWORD}
        )
        status, headers, body = http_request(
            "POST",
            f"{DASHBOARD_BASE_URL}/auth/login?{params}",
            timeout=15,
        )
        if status != HTTPStatus.OK:
            raise RuntimeError(f"Dashboard login failed: HTTP {status}")
        payload = json.loads(body.decode("utf-8"))
        if not payload.get("success"):
            raise RuntimeError(f"Dashboard login failed: {payload.get('msg')}")
        cookie = headers.get("Set-Cookie") or headers.get("set-cookie")
        if not cookie:
            raise RuntimeError("Dashboard login did not return Set-Cookie")
        self.cookie = cookie.split(";", 1)[0]
        self.login_at = time.time()

    def headers(self):
        self.ensure_login()
        return {"Cookie": self.cookie}


dashboard_session = DashboardSession()
nacos_token = {"value": None, "login_at": 0}
rule_cache = {}


def login_nacos():
    if nacos_token["value"] and time.time() - nacos_token["login_at"] < 3600:
        return nacos_token["value"]
    body = urllib.parse.urlencode(
        {"username": NACOS_USERNAME, "password": NACOS_PASSWORD}
    ).encode("utf-8")
    status, _, data = http_request(
        "POST",
        f"{NACOS_BASE_URL}/v1/auth/users/login",
        headers={"Content-Type": "application/x-www-form-urlencoded"},
        body=body,
        timeout=15,
    )
    if status != HTTPStatus.OK:
        raise RuntimeError(f"Nacos login failed: HTTP {status}")
    payload = json.loads(data.decode("utf-8"))
    token = payload.get("accessToken")
    if not token:
        raise RuntimeError("Nacos login did not return accessToken")
    nacos_token["value"] = token
    nacos_token["login_at"] = time.time()
    return token


def query_dashboard_rules(app, rule_type):
    machine = select_machine(query_app_machines(app))
    params = urllib.parse.urlencode(
        {"app": app, "ip": machine.get("ip"), "port": machine.get("port")}
    )
    status, _, data = http_request(
        "GET",
        f"{DASHBOARD_BASE_URL}{RULE_ENDPOINTS[rule_type]}?{params}",
        headers=dashboard_session.headers(),
        timeout=15,
    )
    if status != HTTPStatus.OK:
        raise RuntimeError(f"Query {rule_type} rules for {app} failed: HTTP {status}")
    payload = json.loads(data.decode("utf-8"))
    if not payload.get("success"):
        raise RuntimeError(f"Query {rule_type} rules for {app} failed: {payload.get('msg')}")
    return payload.get("data") or []


def query_app_machines(app):
    quoted_app = urllib.parse.quote(app, safe="")
    status, _, data = http_request(
        "GET",
        f"{DASHBOARD_BASE_URL}/app/{quoted_app}/machines.json",
        headers=dashboard_session.headers(),
        timeout=15,
    )
    if status != HTTPStatus.OK:
        raise RuntimeError(f"Query machines for {app} failed: HTTP {status}")
    payload = json.loads(data.decode("utf-8"))
    if not payload.get("success"):
        raise RuntimeError(f"Query machines for {app} failed: {payload.get('msg')}")
    return payload.get("data") or []


def publish_nacos_config(data_id, content):
    token = login_nacos()
    body = urllib.parse.urlencode(
        {
            "dataId": data_id,
            "groupName": NACOS_GROUP,
            "namespaceId": NACOS_NAMESPACE,
            "type": "json",
            "content": content,
            "accessToken": token,
        }
    ).encode("utf-8")
    status, _, data = http_request(
        "POST",
        f"{NACOS_BASE_URL}/v3/admin/cs/config",
        headers={"Content-Type": "application/x-www-form-urlencoded"},
        body=body,
        timeout=15,
    )
    if not is_nacos_publish_success(status, data):
        text = data.decode("utf-8", errors="replace")
        raise RuntimeError(f"Publish {data_id} failed: HTTP {status} {text[:200]}")


def query_nacos_config(data_id):
    token = login_nacos()
    params = urllib.parse.urlencode(
        {
            "dataId": data_id,
            "groupName": NACOS_GROUP,
            "namespaceId": NACOS_NAMESPACE,
            "accessToken": token,
        }
    )
    status, _, data = http_request(
        "GET",
        f"{NACOS_BASE_URL}/v3/admin/cs/config?{params}",
        timeout=15,
    )
    if status != HTTPStatus.OK:
        raise RuntimeError(f"Query {data_id} failed: HTTP {status}")
    payload = json.loads(data.decode("utf-8"))
    return (payload.get("data") or {}).get("content") or "[]"


def is_nacos_publish_success(status, data):
    if status != HTTPStatus.OK:
        return False
    text = data.decode("utf-8", errors="replace")
    try:
        payload = json.loads(text)
    except json.JSONDecodeError:
        return '"success":true' in text
    return payload.get("success") is True or (
        payload.get("code") == 0 and payload.get("data") is True
    )


def remove_sanitized_rule_from_content(rule_type, content, deleted_rule):
    deleted = sanitize_rule(rule_type, deleted_rule)
    if not deleted:
        return content
    try:
        rules = json.loads(content or "[]")
    except json.JSONDecodeError:
        rules = []
    kept = [
        rule for rule in rules
        if not sanitized_rule_matches(rule_type, rule, deleted)
    ]
    return json.dumps(kept, ensure_ascii=False, separators=(",", ":"))


def sanitized_rule_matches(rule_type, existing, deleted):
    existing = sanitize_rule(rule_type, existing)
    for key, value in deleted.items():
        if key in existing and existing[key] != value:
            return False
    keys = ("resource", "limitApp")
    return all(key in existing and existing.get(key) == deleted.get(key) for key in keys)


def parse_rule_mutation(path):
    normalized = urllib.parse.urlsplit(path).path
    for rule_type, prefix in RULE_MUTATION_PATHS.items():
        if normalized == prefix:
            return rule_type, None
        delete_prefix = f"{prefix}/"
        if normalized.startswith(delete_prefix):
            return rule_type, normalized[len(delete_prefix):]
    return None, None


def find_dashboard_rule_by_id(app, rule_type, rule_id):
    if not rule_id:
        return None
    cached = find_cached_rule(app, rule_type, rule_id)
    if cached:
        return cached
    expected = str(rule_id)
    for rule in query_dashboard_rules(app, rule_type):
        if str(rule.get("id")) == expected:
            return rule
    return None


def find_cached_rule(app, rule_type, rule_id):
    return rule_cache.get((app, rule_type, str(rule_id)))


def cache_rules_from_dashboard_response(rule_type, data):
    try:
        payload = json.loads(data.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError):
        return
    rules = payload.get("data")
    if isinstance(rules, dict):
        rules = [rules]
    if not isinstance(rules, list):
        return
    for rule in rules:
        if not isinstance(rule, dict):
            continue
        app = rule.get("app")
        rule_id = rule.get("id")
        if app is not None and rule_id is not None:
            rule_cache[(app, rule_type, str(rule_id))] = rule


def remove_rule_from_nacos(app, rule_type, deleted_rule):
    data_id = build_data_id(DATA_ID_PATTERN, app, rule_type)
    content = query_nacos_config(data_id)
    updated = remove_sanitized_rule_from_content(rule_type, content, deleted_rule)
    publish_nacos_config(data_id, updated)
    print(f"removed {app} {rule_type} -> {data_id}", flush=True)


def sync_app_rule_type(app, rule_type):
    rules = query_dashboard_rules(app, rule_type)
    sanitized = [sanitize_rule(rule_type, rule) for rule in rules]
    content = json.dumps(sanitized, ensure_ascii=False, separators=(",", ":"))
    data_id = build_data_id(DATA_ID_PATTERN, app, rule_type)
    publish_nacos_config(data_id, content)
    print(f"synced {app} {rule_type} -> {data_id} ({len(sanitized)} rules)", flush=True)


def sync_apps(apps):
    for app in apps:
        for rule_type in RULE_ENDPOINTS:
            try:
                sync_app_rule_type(app, rule_type)
            except NoDashboardMachine as exc:
                print(f"sync skipped for {app}: {exc}", flush=True)
                break
            except Exception as exc:
                print(f"sync failed for {app} {rule_type}: {exc}", flush=True)


def discover_apps():
    status, _, data = http_request(
        "GET",
        f"{DASHBOARD_BASE_URL}/app/names.json",
        headers=dashboard_session.headers(),
        timeout=15,
    )
    if status != HTTPStatus.OK:
        return SYNC_APPS
    payload = json.loads(data.decode("utf-8"))
    apps = payload.get("data") or []
    return [app for app in apps if app and app != "sentinel-dashboard"]


def proxied_headers(handler):
    headers = {}
    for key, value in handler.headers.items():
        if key.lower() not in HOP_BY_HOP_HEADERS:
            headers[key] = value
    return headers


class ProxyHandler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def do_GET(self):
        self.proxy()

    def do_POST(self):
        self.proxy()

    def do_PUT(self):
        self.proxy()

    def do_DELETE(self):
        self.proxy()

    def do_HEAD(self):
        self.proxy(send_body=False)

    def proxy(self, send_body=True):
        length = int(self.headers.get("Content-Length", "0") or "0")
        body = self.rfile.read(length) if length else None
        target = f"{DASHBOARD_BASE_URL}{self.path}"
        apps = discover_apps() if not SYNC_APPS else SYNC_APPS
        delete_mutation = None
        if self.command == "DELETE":
            rule_type, rule_id = parse_rule_mutation(self.path)
            if rule_type and rule_id and len(apps) == 1:
                deleted_rule = find_dashboard_rule_by_id(apps[0], rule_type, rule_id)
                delete_mutation = (apps[0], rule_type, deleted_rule)
        status, headers, response_body = http_request(
            self.command,
            target,
            headers=proxied_headers(self),
            body=body,
            timeout=30,
        )
        self.send_response(status)
        for key, value in headers.items():
            if key.lower() not in HOP_BY_HOP_HEADERS:
                self.send_header(key, value)
        self.send_header("Content-Length", str(len(response_body)))
        self.end_headers()
        if send_body:
            self.wfile.write(response_body)
        rule_type, _ = parse_rule_mutation(self.path)
        if status == HTTPStatus.OK and rule_type:
            cache_rules_from_dashboard_response(rule_type, response_body)
        if self.command in {"POST", "PUT", "DELETE"} and self.is_rule_mutation():
            if status == HTTPStatus.OK and delete_mutation and delete_mutation[2]:
                remove_rule_from_nacos(*delete_mutation)
            else:
                sync_apps(apps)

    def is_rule_mutation(self):
        path = urllib.parse.urlsplit(self.path).path
        return any(path.startswith(prefix) for prefix in RULE_MUTATION_PREFIXES)


def run_periodic_sync():
    while True:
        time.sleep(SYNC_INTERVAL_SECONDS)
        apps = discover_apps() if not SYNC_APPS else SYNC_APPS
        sync_apps(apps)


def main():
    import threading

    threading.Thread(target=run_periodic_sync, daemon=True).start()
    server = ThreadingHTTPServer(("0.0.0.0", 8080), ProxyHandler)
    print("sentinel nacos proxy listening on 8080", flush=True)
    server.serve_forever()


if __name__ == "__main__":
    main()
