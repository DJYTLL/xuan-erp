import importlib.util
import io
import pathlib
import unittest
from contextlib import redirect_stdout


MODULE_PATH = pathlib.Path(__file__).with_name("sentinel_nacos_proxy.py")


def load_module():
    spec = importlib.util.spec_from_file_location("sentinel_nacos_proxy", MODULE_PATH)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class SentinelNacosProxyTests(unittest.TestCase):
    def test_data_id_uses_app_name_and_rule_type(self):
        module = load_module()

        data_id = module.build_data_id(
            "{app}-sentinel-{rule_type}-rules.json",
            "xuan-tenant",
            "param-flow",
        )

        self.assertEqual(data_id, "xuan-tenant-sentinel-param-flow-rules.json")

    def test_flow_rule_sanitizer_removes_dashboard_metadata(self):
        module = load_module()
        rule = {
            "id": 12,
            "app": "xuan-tenant",
            "ip": "127.0.0.1",
            "port": 8719,
            "resource": "/api/tenants/{tenantId}",
            "limitApp": "default",
            "grade": 1,
            "count": 20,
            "strategy": 0,
            "controlBehavior": 0,
            "gmtCreate": "ignored",
        }

        sanitized = module.sanitize_rule("flow", rule)

        self.assertEqual(
            sanitized,
            {
                "resource": "/api/tenants/{tenantId}",
                "limitApp": "default",
                "grade": 1,
                "count": 20,
                "strategy": 0,
                "controlBehavior": 0,
            },
        )

    def test_select_machine_prefers_healthy_instance(self):
        module = load_module()
        machines = [
            {"ip": "10.0.0.2", "port": 8720, "healthy": False},
            {"ip": "10.0.0.1", "port": 8719, "healthy": True},
        ]

        machine = module.select_machine(machines)

        self.assertEqual(machine, {"ip": "10.0.0.1", "port": 8719, "healthy": True})

    def test_param_flow_rule_sanitizer_unwraps_dashboard_rule_payload(self):
        module = load_module()
        rule = {
            "id": 18,
            "app": "xuan-tenant",
            "rule": {
                "resource": "/api/tenants/{tenantId}",
                "limitApp": "default",
                "grade": 1,
                "paramIdx": 0,
                "count": 5,
                "durationInSec": 1,
                "controlBehavior": 0,
            },
        }

        sanitized = module.sanitize_rule("param-flow", rule)

        self.assertEqual(
            sanitized,
            {
                "resource": "/api/tenants/{tenantId}",
                "limitApp": "default",
                "grade": 1,
                "paramIdx": 0,
                "count": 5,
                "durationInSec": 1,
                "controlBehavior": 0,
            },
        )

    def test_sync_apps_skips_remaining_rule_types_when_dashboard_has_no_machine(self):
        module = load_module()
        calls = []

        def fake_sync_app_rule_type(app, rule_type):
            calls.append((app, rule_type))
            raise module.NoDashboardMachine("Dashboard has no machine for app")

        module.sync_app_rule_type = fake_sync_app_rule_type

        with redirect_stdout(io.StringIO()) as output:
            module.sync_apps(["xuan-tenant"])

        self.assertEqual(calls, [("xuan-tenant", "flow")])
        self.assertIn("sync skipped for xuan-tenant", output.getvalue())

    def test_nacos_v3_success_payload_is_accepted(self):
        module = load_module()

        self.assertTrue(
            module.is_nacos_publish_success(
                200,
                b'{"code":0,"message":"success","data":true}',
            )
        )

    def test_remove_sanitized_rule_from_nacos_content(self):
        module = load_module()
        content = (
            '[{"resource":"/keep","limitApp":"default","grade":1,"count":1.0},'
            '{"resource":"/delete","limitApp":"default","grade":1,"count":2.0}]'
        )
        deleted_rule = {
            "id": 7,
            "resource": "/delete",
            "limitApp": "default",
            "grade": 1,
            "count": 2.0,
        }

        updated = module.remove_sanitized_rule_from_content(
            "flow",
            content,
            deleted_rule,
        )

        self.assertEqual(
            updated,
            '[{"resource":"/keep","limitApp":"default","grade":1,"count":1.0}]',
        )

    def test_delete_rule_mutation_path_identifies_flow_rule_id(self):
        module = load_module()

        mutation = module.parse_rule_mutation("/v2/flow/rule/17")

        self.assertEqual(mutation, ("flow", "17"))

    def test_cache_rule_from_dashboard_response_by_app_type_and_id(self):
        module = load_module()
        body = (
            b'{"success":true,"data":{"id":9,"app":"xuan-tenant",'
            b'"resource":"/cached","limitApp":"default"}}'
        )

        module.cache_rules_from_dashboard_response("flow", body)

        self.assertEqual(
            module.find_cached_rule("xuan-tenant", "flow", "9"),
            {
                "id": 9,
                "app": "xuan-tenant",
                "resource": "/cached",
                "limitApp": "default",
            },
        )


if __name__ == "__main__":
    unittest.main()
