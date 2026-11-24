import json
import os
from pathlib import Path
from playwright.sync_api import sync_playwright, expect

OUT = Path("evidence")
OUT.mkdir(exist_ok=True)
BASE = "http://127.0.0.1:7900"
with sync_playwright() as pw:
    browser = pw.chromium.launch()
    context = browser.new_context(viewport={"width": 1600, "height": 1000}, device_scale_factor=1, locale="zh-CN")
    page = context.new_page()
    page.set_default_timeout(30000)
    try:
        page.goto(BASE + "/web/")
        page.locator('input[autocomplete="username"]').fill("admin")
        page.locator('input[autocomplete="current-password"]').fill(os.environ["DEMO_PASSWORD"])
        page.locator('input[autocomplete="current-password"]').press("Enter")
        card = page.get_by_text("CertiControl 控制系统诊断", exact=True)
        expect(card.first).to_be_visible(timeout=60000)
        page.screenshot(path=OUT / "01-app-list.png", full_page=True)
        card.first.click()
        page.get_by_text("系统模型", exact=True).first.click()
        page.get_by_role("button", name="新增", exact=True).first.click()
        name = page.get_by_placeholder("请输入系统名称", exact=True)
        name.fill("双执行器状态反馈系统")
        for label, value in [("状态矩阵 A", "1,0;0,-2"), ("输入矩阵 B", "1,0;0,1"), ("输出矩阵 C", "1,0;0,1"), ("状态反馈 K（可选）", "3,0;0,0")]:
            page.get_by_placeholder("请输入" + label, exact=True).fill(value)
        page.screenshot(path=OUT / "02-model-form.png", full_page=True)
        # Native Qingzhou form submission, with response captured directly from the actual app.
        with page.expect_response(lambda r: "/registry/invoke/" in r.url and "/add" in r.url and r.request.method == "POST") as added:
            page.get_by_role("button", name="确定", exact=True).last.click()
        result = added.value.json()
        assert result.get("success") is True, result
        expect(page.get_by_text("双执行器状态反馈系统", exact=True).first).to_be_visible()
        expect(page.get_by_text("HURWITZ", exact=True).first).to_be_visible()
        page.screenshot(path=OUT / "03-system-diagnosis.png", full_page=True)
        row = page.get_by_role("row").filter(has_text="双执行器状态反馈系统")
        with page.expect_response(lambda r: "/registry/invoke/" in r.url and "/faults" in r.url and r.request.method == "POST") as fault:
            row.get_by_text("单通道故障分析", exact=True).click()
        response = fault.value.json()
        assert response.get("success") is True, response
        data = response["data"]
        assert data["actuator 1 closed-loop stability"] == "UNSTABLE", data
        assert float(data["actuator 1 closed-loop margin"]) == -1.0, data
        expect(page.get_by_text("UNSTABLE", exact=False).first).to_be_visible()
        page.screenshot(path=OUT / "04-actuator-failure.png", full_page=True)
        (OUT / "fault-response.json").write_text(json.dumps(response, ensure_ascii=False, indent=2))
    except Exception:
        page.screenshot(path=OUT / "failure.png", full_page=True)
        (OUT / "page-visible-text.txt").write_text(page.locator("body").inner_text())
        raise
    finally:
        browser.close()
