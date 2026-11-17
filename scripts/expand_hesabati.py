from pathlib import Path
import base64
import gzip
import re
import sys

path = Path("index.html")
wrapper = path.read_text(encoding="utf-8")
match = re.search(r'atob\("([A-Za-z0-9+/=]+)"\)', wrapper)
if not match:
    raise SystemExit("Could not find compressed app payload in index.html")

page = gzip.decompress(base64.b64decode(match.group(1))).decode("utf-8")
if "</style>" not in page or '<h2 class="section-title">ملخص اليوم</h2>' not in page:
    raise SystemExit("The decompressed app did not contain the expected dashboard markers")

css = """.quick-section{margin-top:22px}.quick-section .section-title{margin:0 0 11px}.quick-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}.quick-link{display:flex;align-items:center;gap:11px;width:100%;min-height:78px;padding:12px;text-align:right;background:#fff;border:1px solid #e1e9e6;border-radius:15px;color:#183536;font:inherit;font-weight:700;box-shadow:0 2px 9px #163b350a;cursor:pointer}.quick-link:active{transform:scale(.985)}.quick-icon{width:46px;height:46px;flex:0 0 46px;display:grid;place-items:center;border-radius:13px;background:#e8f4ef;font-size:24px}.quick-link span:last-child{font-size:14px;line-height:1.5}.quick-link small{display:block;color:#6d8280;font-size:11px;font-weight:500}@media(min-width:700px){.quick-grid{grid-template-columns:repeat(4,minmax(0,1fr))}}"""
page = page.replace("</style>", css + "</style>", 1)

marker = '<h2 class="section-title">ملخص اليوم</h2>'
sections = """<div class="quick-section"><h2 class="section-title">إدارة الرصيد</h2><div class="quick-grid"><button class="quick-link" type="button" data-panel="credit" data-target="stock"><span class="quick-icon">💰</span><span>رصيد داخل<small>إضافة رصيد جديد</small></span></button><button class="quick-link" type="button" data-panel="credit" data-target="transfer"><span class="quick-icon">🔄</span><span>تحويل رصيد<small>تسجيل عملية تحويل</small></span></button><button class="quick-link" type="button" data-panel="expenses" data-target="expenses"><span class="quick-icon">🧾</span><span>مصروفات<small>تسجيل المصروفات</small></span></button><button class="quick-link" type="button" data-panel="credit" data-target="creditHistory"><span class="quick-icon">📒</span><span>سجل عمليات الرصيد<small>عرض وتعديل العمليات</small></span></button></div></div><div class="quick-section"><h2 class="section-title">شحن الهواتف</h2><div class="quick-grid"><button class="quick-link" type="button" data-panel="recharge" data-target="recharge"><span class="quick-icon">📱</span><span>قسم شحن الهواتف<small>تسجيل عملية شحن</small></span></button><button class="quick-link" type="button" data-panel="recharge" data-target="rechargeHistory"><span class="quick-icon">📋</span><span>سجل الشحن<small>عرض وتعديل عمليات الشحن</small></span></button></div></div>"""
if page.count(marker) != 1:
    raise SystemExit("Dashboard summary marker is missing or duplicated")
page = page.replace(marker, sections + marker, 1)

interaction = """<script>(function(){const targets={stock:function(){return document.querySelector("#credit .card")},transfer:function(){return document.querySelectorAll("#credit .card")[1]},expenses:function(){return document.querySelector("#expenses .card")},creditHistory:function(){return document.querySelector("#credit .table-wrap")},recharge:function(){return document.querySelector("#recharge .card")},rechargeHistory:function(){return document.querySelector("#recharge .table-wrap")}};document.querySelectorAll(".quick-link").forEach(function(b){b.addEventListener("click",function(){const tab=b.getAttribute("data-panel");document.querySelectorAll("#nav button").forEach(function(x){x.classList.toggle("active",x.getAttribute("data-tab")===tab)});document.querySelectorAll(".panel").forEach(function(x){x.classList.toggle("active",x.id===tab)});const el=targets[b.getAttribute("data-target")]();if(el)setTimeout(function(){el.scrollIntoView({behavior:"smooth",block:"start"})},80)})})})();</script>"""
if "</body>" not in page:
    raise SystemExit("App closing body tag is missing")
page = page.replace("</body>", interaction + "</body>", 1)

if 'name="theme-color"' in page:
    page = page.replace('<meta name="theme-color" content="#123b3b">', '<meta name="theme-color" content="#123b3b"><meta http-equiv="Cache-Control" content="no-cache, no-store, must-revalidate"><meta http-equiv="Pragma" content="no-cache"><meta http-equiv="Expires" content="0">', 1)

Path("/tmp/index-expanded.html").write_text(page, encoding="utf-8")
if "تعذر فك ضغط ملفات التطبيق" in page or "DecompressionStream" in page or "atob(" in page:
    raise SystemExit("The expanded app still contains the compressed bootstrapper")
if any(label not in page for label in ["رصيد داخل", "تحويل رصيد", "مصروفات", "سجل عمليات الرصيد", "قسم شحن الهواتف", "سجل الشحن"]):
    raise SystemExit("One or more quick-action labels are missing")

print("EXPANDED_APP_BEGIN")
print(page)
print("EXPANDED_APP_END")
