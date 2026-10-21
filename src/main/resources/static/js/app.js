(function () {
    "use strict";

    var API = "/api";
    var sessionId = localStorage.getItem("ohcp_session_id") || "";

    var messagesEl = document.getElementById("messages");
    var detailEl = document.getElementById("detail");
    var formEl = document.getElementById("chatForm");
    var messageEl = document.getElementById("message");
    var cropEl = document.getElementById("crop");
    var partEl = document.getElementById("part");
    var environmentEl = document.getElementById("environment");
    var sendBtn = document.getElementById("sendBtn");
    var kbStatEl = document.getElementById("kbStat");
    var newSessionBtn = document.getElementById("newSessionBtn");

    function esc(value) {
        return String(value == null ? "" : value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;");
    }

    function pct(value) {
        return Math.round(Number(value) * 100) + "%";
    }

    function greeting() {
        addAssistantHtml(
            "<b>您好，我是农作物缺素症识别智能体。</b><br>" +
            "请描述作物类型、症状部位与症状表现，我将先检索本地知识库，再为您输出多个候选缺素类型与科学施肥建议。<br>" +
            "您也可以继续追问缺素细节、施肥用量或不同作物的适配方案（支持多轮对话）。"
        );
    }

    function addMessage(role, html) {
        var wrap = document.createElement("div");
        wrap.className = "msg " + role;
        var bubble = document.createElement("div");
        bubble.className = "bubble";
        bubble.innerHTML = html;
        wrap.appendChild(bubble);
        messagesEl.appendChild(wrap);
        messagesEl.scrollTop = messagesEl.scrollHeight;
    }

    function addUserHtml(text) {
        addMessage("user", esc(text));
    }

    function addAssistantHtml(html) {
        addMessage("assistant", html);
    }

    function renderCandidates(candidates) {
        if (!candidates || !candidates.length) return "";
        var items = candidates.map(function (c, i) {
            var label = i === 0 ? "最佳匹配" : "候选";
            return '<div class="cand">' +
                '<div class="name">' + label + ' · ' + esc(c.name) + '</div>' +
                '<div class="elem">元素 ' + esc(c.element) + '</div>' +
                '<div class="bar"><div class="fill" style="width:' + pct(c.confidence) + '"></div></div>' +
                '<div class="pct">置信度 ' + pct(c.confidence) + '</div>' +
                '</div>';
        }).join("");
        return '<div class="candidates">' + items + '</div>';
    }

    function listItems(items) {
        if (!items || !items.length) return "<p>请结合当地农技部门测土配方施肥意见实施。</p>";
        var html = "<ol>";
        items.forEach(function (item) { html += "<li>" + esc(item) + "</li>"; });
        html += "</ol>";
        return html;
    }

    function renderAdvice(advice) {
        if (!advice) return "";
        return '<div class="advice">' +
            '<section><h3>缺素名称</h3><p><b>' + esc(advice.name) + '</b>（元素 ' + esc(advice.element) + '）</p></section>' +
            '<section><h3>缺素简介</h3><div class="desc">' + esc(advice.description) + '</div></section>' +
            '<section><h3>农艺调控手段</h3>' + listItems(advice.agronomic) + '</section>' +
            '<section><h3>有机 / 生物肥施用方案</h3>' + listItems(advice.organicFertilizer) + '</section>' +
            '<section><h3>化学施肥方案</h3>' + listItems(advice.chemicalFertilizer) + '</section>' +
            '<section><h3>施肥安全与减量提醒</h3>' + listItems(advice.safety) + '</section>' +
            '</div>';
    }

    function extractIdeology(answer) {
        if (!answer) return "";
        var lines = answer.split("\n");
        for (var i = lines.length - 1; i >= 0; i--) {
            if (lines[i].indexOf("🌾") >= 0) {
                return lines[i].replace("🌾", "").trim();
            }
        }
        return "";
    }

    function renderMeta(resp) {
        var parts = [];
        parts.push("知识库命中 " + resp.knowledgeHits + " 条");
        parts.push("耗时 " + resp.elapsedMs + " ms");
        parts.push(resp.llmEnhanced ? "大模型增强" : "本地模板输出");
        if (resp.sources && resp.sources.length) {
            parts.push(resp.sources.join(" · "));
        }
        return '<div class="meta">' + parts.map(esc).join(" · ") + '</div>';
    }

    function renderAssistant(resp) {
        var html = renderCandidates(resp.candidates);
        html += renderAdvice(resp.advice);
        var ideology = extractIdeology(resp.answer);
        if (ideology) {
            html += '<div class="ideology">🌾 ' + esc(ideology) + '</div>';
        }
        html += renderMeta(resp);
        if (resp.answer) {
            html += '<details style="margin-top:8px"><summary style="font-size:12px;color:#6b7a71;cursor:pointer">查看完整诊断文本</summary>' +
                '<pre>' + esc(resp.answer) + '</pre></details>';
        }
        addAssistantHtml(html);
        renderDetail(resp);
    }

    function renderDetail(resp) {
        var html = "";
        if (resp.candidates && resp.candidates.length) {
            resp.candidates.forEach(function (c, i) {
                html += '<div class="detail-cand">' +
                    '<div class="row"><span>' + (i === 0 ? "★ " : "") + esc(c.name) + '（' + esc(c.element) + '）</span>' +
                    '<span>' + pct(c.confidence) + '</span></div>' +
                    '<div class="bar"><div class="fill" style="width:' + pct(c.confidence) + '"></div></div>' +
                    '</div>';
            });
        }
        if (resp.advice) {
            html += renderAdvice(resp.advice);
        }
        if (resp.sources && resp.sources.length) {
            html += '<div class="meta">来源：' + esc(resp.sources.join(" · ")) + '</div>';
        }
        detailEl.innerHTML = html || '<p class="placeholder">暂无详情。</p>';
    }

    function setSending(flag) {
        sendBtn.disabled = flag;
        sendBtn.textContent = flag ? "检索中…" : "识别诊断";
    }

    function send(text, crop, part, environment) {
        text = (text || "").trim();
        if (!text) {
            messageEl.focus();
            return;
        }
        addUserHtml(text);
        setSending(true);

        var body = { sessionId: sessionId, message: text };
        if (crop) body.crop = crop;
        if (part) body.part = part;
        if (environment) body.environment = environment;

        fetch(API + "/chat", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(body)
        })
            .then(function (r) {
                if (!r.ok) throw new Error("HTTP " + r.status);
                return r.json();
            })
            .then(function (resp) {
                sessionId = resp.sessionId || sessionId;
                localStorage.setItem("ohcp_session_id", sessionId);
                renderAssistant(resp);
            })
            .catch(function (err) {
                addAssistantHtml("请求失败：" + esc(err.message) + "，请确认后端服务已启动（http://localhost:8080）。");
            })
            .finally(function () {
                setSending(false);
                messageEl.value = "";
                messageEl.focus();
            });
    }

    function loadStats() {
        fetch(API + "/knowledge/stats")
            .then(function (r) { return r.json(); })
            .then(function (data) {
                kbStatEl.textContent = "知识库 " + data.total + " 条";
            })
            .catch(function () {
                kbStatEl.textContent = "知识库未连接";
            });
    }

    formEl.addEventListener("submit", function (e) {
        e.preventDefault();
        send(messageEl.value, cropEl.value, partEl.value, environmentEl.value);
    });

    document.querySelectorAll(".chip").forEach(function (chip) {
        chip.addEventListener("click", function () {
            messageEl.value = chip.getAttribute("data-example") || "";
            send(messageEl.value, "", "", "");
        });
    });

    newSessionBtn.addEventListener("click", function () {
        sessionId = "";
        localStorage.removeItem("ohcp_session_id");
        messagesEl.innerHTML = "";
        detailEl.innerHTML = '<p class="placeholder">识别结果将在这里结构化展示。</p>';
        greeting();
    });

    greeting();
    loadStats();
})();