# SkyWalking Java Agent usage

Agent path from the backend directory:

```text
./skywalking-agent/skywalking-agent.jar
```

Windows:

```powershell
cd D:\xuan-erp\backend
.\scripts\run-with-skywalking.ps1 xuan-iam
```

Linux:

```sh
cd /path/to/xuan-erp/backend
chmod +x ./scripts/run-with-skywalking.sh
./scripts/run-with-skywalking.sh xuan-iam
```

The default OAP gRPC target is `duaoyunxuan.top:9050`.
