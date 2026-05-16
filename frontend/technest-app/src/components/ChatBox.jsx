import { useState } from "react";
import axios from "axios";
import { buildApiUrl } from "../lib/api.js";

function MessageContent({ content }) {
  const lines = String(content || "").split("\n").filter(Boolean);

  if (lines.length <= 1) {
    return <span>{content}</span>;
  }

  return (
    <div style={{ display: "grid", gap: "8px" }}>
      {lines.map((line, index) => {
        const isBullet = line.trim().startsWith("-");

        return (
          <div
            key={`${line}-${index}`}
            style={{
              display: isBullet ? "grid" : "block",
              gridTemplateColumns: isBullet ? "14px 1fr" : undefined,
              gap: isBullet ? "6px" : undefined,
              alignItems: "start",
            }}
          >
            {isBullet ? (
              <>
                <span style={{ color: "#2563eb", fontWeight: 700 }}>•</span>
                <span>{line.replace(/^-+\s*/, "")}</span>
              </>
            ) : (
              <span style={{ fontWeight: index === 0 ? 700 : 400 }}>{line}</span>
            )}
          </div>
        );
      })}
    </div>
  );
}

export default function ChatBox() {
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState([
    {
      role: "assistant",
      content: "Xin chào. Tôi có thể hỗ trợ thông tin sản phẩm cho bạn.",
    },
  ]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [lastProductQuestion, setLastProductQuestion] = useState("");

  const sendMessage = async () => {
    const trimmedInput = input.trim();
    if (!trimmedInput) return;

    setMessages((prev) => [...prev, { role: "user", content: trimmedInput }]);
    setInput("");
    setLoading(true);

    try {
      const response = await axios.post(buildApiUrl("/api/ai/chat"), {
        message: trimmedInput,
        context: lastProductQuestion,
      });

      setMessages((prev) => [
        ...prev,
        {
          role: "assistant",
          content: response.data.reply,
        },
      ]);
    } catch (error) {
      setMessages((prev) => [
        ...prev,
        {
          role: "assistant",
          content: "Không thể kết nối AI.",
        },
      ]);
    }

    if (isProductQuestion(trimmedInput)) {
      setLastProductQuestion(trimmedInput);
    }

    setLoading(false);
  };

  return (
    <>
      <button
        onClick={() => setIsOpen(!isOpen)}
        aria-label="Mở chat AI"
        title="Chat với AI"
        style={{
          position: "fixed",
          bottom: "20px",
          right: "20px",
          width: "65px",
          height: "65px",
          borderRadius: "50%",
          background: "#2563eb",
          color: "white",
          border: "none",
          fontSize: "28px",
          cursor: "pointer",
          zIndex: 999999,
          boxShadow: "0 10px 30px rgba(0,0,0,0.3)",
        }}
      >
        💬
      </button>

      {isOpen && (
        <div
          style={{
            position: "fixed",
            bottom: "100px",
            right: "20px",
            width: "min(380px, calc(100vw - 40px))",
            height: "min(600px, calc(100vh - 130px))",
            background: "white",
            borderRadius: "18px",
            overflow: "hidden",
            zIndex: 999999,
            boxShadow: "0 10px 30px rgba(0,0,0,0.25)",
            border: "1px solid #ddd",
            display: "flex",
            flexDirection: "column",
          }}
        >
          <div
            style={{
              background: "#2563eb",
              color: "white",
              padding: "16px",
              fontWeight: "bold",
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
            }}
          >
            <div>AI Assistant</div>

            <button
              onClick={() => setIsOpen(false)}
              aria-label="Đóng chat"
              style={{
                background: "transparent",
                border: "none",
                color: "white",
                fontSize: "24px",
                cursor: "pointer",
              }}
            >
              ×
            </button>
          </div>

          <div
            style={{
              flex: 1,
              overflowY: "auto",
              padding: "15px",
              background: "#f5f5f5",
            }}
          >
            {messages.map((msg, index) => (
              <div
                key={index}
                style={{
                  display: "flex",
                  justifyContent: msg.role === "user" ? "flex-end" : "flex-start",
                  marginBottom: "10px",
                }}
              >
                <div
                  style={{
                    background: msg.role === "user" ? "#2563eb" : "white",
                    color: msg.role === "user" ? "white" : "#111827",
                    padding: "12px 14px",
                    borderRadius: "16px",
                    maxWidth: msg.role === "user" ? "80%" : "88%",
                    lineHeight: 1.45,
                    fontSize: "14px",
                    whiteSpace: "pre-wrap",
                    overflowWrap: "anywhere",
                    boxShadow:
                      msg.role === "user"
                        ? "none"
                        : "0 1px 3px rgba(15,23,42,0.08)",
                  }}
                >
                  <MessageContent content={msg.content} />
                </div>
              </div>
            ))}

            {loading && (
              <div
                style={{
                  display: "inline-flex",
                  background: "white",
                  color: "#475569",
                  padding: "10px 12px",
                  borderRadius: "14px",
                  fontSize: "14px",
                  boxShadow: "0 1px 3px rgba(15,23,42,0.08)",
                }}
              >
                AI đang trả lời...
              </div>
            )}
          </div>

          <div
            style={{
              display: "flex",
              padding: "12px",
              borderTop: "1px solid #ddd",
              gap: "10px",
            }}
          >
            <textarea
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter" && !e.shiftKey) {
                  e.preventDefault();
                  sendMessage();
                }
              }}
              placeholder="Hỏi sản phẩm..."
              rows={1}
              style={{
                flex: 1,
                minWidth: 0,
                maxHeight: "96px",
                padding: "12px",
                borderRadius: "18px",
                border: "1px solid #ccc",
                outline: "none",
                resize: "none",
                lineHeight: 1.35,
                fontFamily: "inherit",
                fontSize: "14px",
                overflowY: "auto",
              }}
            />

            <button
              onClick={sendMessage}
              disabled={loading}
              style={{
                background: loading ? "#94a3b8" : "#2563eb",
                color: "white",
                border: "none",
                borderRadius: "999px",
                padding: "0 18px",
                cursor: loading ? "not-allowed" : "pointer",
              }}
            >
              Gửi
            </button>
          </div>
        </div>
      )}
    </>
  );
}

function isProductQuestion(message) {
  const value = message.toLowerCase();
  return [
    "mua",
    "sản phẩm",
    "san pham",
    "điện thoại",
    "dien thoai",
    "laptop",
    "lap",
    "màn hình",
    "man hinh",
    "tai nghe",
    "headphone",
    "phụ kiện",
    "phu kien",
    "xiaomi",
    "iphone",
    "samsung",
    "so sánh",
    "so sanh",
  ].some((term) => value.includes(term));
}
