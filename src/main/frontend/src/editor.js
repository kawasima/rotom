import { EditorView, keymap, placeholder, lineNumbers } from "@codemirror/view";
import { markdown, markdownLanguage } from "@codemirror/lang-markdown";
import { languages } from "@codemirror/language-data";
import { defaultKeymap, indentWithTab, history, historyKeymap } from "@codemirror/commands";
import { closeBrackets, closeBracketsKeymap } from "@codemirror/autocomplete";
import { searchKeymap, highlightSelectionMatches } from "@codemirror/search";
import { syntaxHighlighting, defaultHighlightStyle, bracketMatching } from "@codemirror/language";

// --- Toolbar helpers ---

function wrapSelection(view, before, after) {
  const { from, to } = view.state.selection.main;
  const selected = view.state.sliceDoc(from, to);
  const replacement = before + (selected || "text") + (after || before);
  view.dispatch({
    changes: { from, to, insert: replacement },
    selection: { anchor: from + before.length, head: from + before.length + (selected || "text").length },
  });
  view.focus();
}

function prefixLines(view, prefix) {
  const { from, to } = view.state.selection.main;
  const doc = view.state.doc;
  const startLine = doc.lineAt(from).number;
  const endLine = doc.lineAt(to).number;
  const changes = [];
  for (let i = startLine; i <= endLine; i++) {
    const line = doc.line(i);
    changes.push({ from: line.from, insert: prefix });
  }
  view.dispatch({ changes });
  view.focus();
}

function insertAtCursor(view, text) {
  const pos = view.state.selection.main.head;
  view.dispatch({
    changes: { from: pos, insert: text },
    selection: { anchor: pos + text.length },
  });
  view.focus();
}

function setHeading(view, level) {
  const { from } = view.state.selection.main;
  const line = view.state.doc.lineAt(from);
  const lineText = line.text;
  const stripped = lineText.replace(/^#+\s*/, "");
  const prefix = "#".repeat(level) + " ";
  view.dispatch({
    changes: { from: line.from, to: line.to, insert: prefix + stripped },
  });
  view.focus();
}

// --- Custom keybindings ---

function boldKeymap(view) {
  wrapSelection(view, "**");
  return true;
}

function italicKeymap(view) {
  wrapSelection(view, "*");
  return true;
}

function linkKeymap(view) {
  const { from, to } = view.state.selection.main;
  const selected = view.state.sliceDoc(from, to);
  const text = selected || "link text";
  const replacement = `[${text}](url)`;
  view.dispatch({
    changes: { from, to, insert: replacement },
    selection: { anchor: from + text.length + 3, head: from + text.length + 6 },
  });
  view.focus();
  return true;
}

function saveKeymap() {
  const form = document.getElementById("gollum-editor-form");
  if (form) form.submit();
  return true;
}

const editorKeymap = keymap.of([
  { key: "Mod-b", run: boldKeymap },
  { key: "Mod-i", run: italicKeymap },
  { key: "Mod-k", run: linkKeymap },
  { key: "Mod-s", run: saveKeymap, preventDefault: true },
]);

// --- Toolbar ---

function initToolbar(view) {
  const bar = document.getElementById("gollum-editor-function-bar");
  if (!bar) return;

  bar.addEventListener("click", (e) => {
    const btn = e.target.closest("[data-action]");
    if (!btn) return;
    e.preventDefault();

    const action = btn.dataset.action;
    switch (action) {
      case "bold": wrapSelection(view, "**"); break;
      case "italic": wrapSelection(view, "*"); break;
      case "code": wrapSelection(view, "`"); break;
      case "strikethrough": wrapSelection(view, "~~"); break;
      case "link": linkKeymap(view); break;
      case "wikilink": wrapSelection(view, "[[", "]]"); break;
      case "image": {
        const { from, to } = view.state.selection.main;
        const selected = view.state.sliceDoc(from, to);
        const text = selected || "alt text";
        const replacement = `![${text}](url)`;
        view.dispatch({
          changes: { from, to, insert: replacement },
          selection: { anchor: from + text.length + 4, head: from + text.length + 7 },
        });
        view.focus();
        break;
      }
      case "ul": prefixLines(view, "- "); break;
      case "ol": {
        const { from, to } = view.state.selection.main;
        const doc = view.state.doc;
        const startLine = doc.lineAt(from).number;
        const endLine = doc.lineAt(to).number;
        const changes = [];
        for (let i = startLine; i <= endLine; i++) {
          const line = doc.line(i);
          changes.push({ from: line.from, insert: `${i - startLine + 1}. ` });
        }
        view.dispatch({ changes });
        view.focus();
        break;
      }
      case "tasklist": prefixLines(view, "- [ ] "); break;
      case "blockquote": prefixLines(view, "> "); break;
      case "hr": insertAtCursor(view, "\n---\n"); break;
      case "h1": setHeading(view, 1); break;
      case "h2": setHeading(view, 2); break;
      case "h3": setHeading(view, 3); break;
      case "table":
        insertAtCursor(view, "\n| Header | Header |\n| ------ | ------ |\n| Cell   | Cell   |\n| Cell   | Cell   |\n");
        break;
    }
  });
}

// --- Preview Tab ---

function initPreviewTab(view) {
  const writeTab = document.getElementById("gollum-editor-tab-write");
  const previewTab = document.getElementById("gollum-editor-tab-preview");
  const previewPane = document.getElementById("gollum-editor-preview-pane");
  const editorContainer = document.getElementById("cm-editor-wrapper");
  const functionBar = document.getElementById("gollum-editor-function-bar");

  if (!writeTab || !previewTab) return;

  writeTab.addEventListener("click", (e) => {
    e.preventDefault();
    writeTab.classList.add("active");
    previewTab.classList.remove("active");
    editorContainer.style.display = "";
    previewPane.style.display = "none";
    if (functionBar) functionBar.style.display = "";
    view.focus();
  });

  previewTab.addEventListener("click", (e) => {
    e.preventDefault();
    previewTab.classList.add("active");
    writeTab.classList.remove("active");
    editorContainer.style.display = "none";
    previewPane.style.display = "";
    if (functionBar) functionBar.style.display = "none";

    previewPane.classList.add("loading");
    previewPane.innerHTML = "<p>Loading preview\u2026</p>";

    const formatSelect = document.getElementById("wiki_format");
    const format = formatSelect ? formatSelect.value : "Markdown";
    const content = view.state.doc.toString();
    const baseUrl = window.baseUrl || "";

    fetch(baseUrl + "/preview", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8" },
      body: "content=" + encodeURIComponent(content) + "&format=" + encodeURIComponent(format),
    })
      .then((res) => {
        if (!res.ok) throw new Error("Preview request failed");
        return res.text();
      })
      .then((html) => {
        previewPane.classList.remove("loading");
        previewPane.innerHTML = html;
      })
      .catch(() => {
        previewPane.classList.remove("loading");
        previewPane.innerHTML = '<p style="color:red;">Preview failed. Please try again.</p>';
      });
  });
}

// --- Unsaved changes warning ---

function initUnsavedWarning() {
  let dirty = false;

  const listener = EditorView.updateListener.of((update) => {
    if (update.docChanged) dirty = true;
  });

  window.addEventListener("beforeunload", (e) => {
    if (dirty) {
      e.preventDefault();
      e.returnValue = "";
    }
  });

  const form = document.getElementById("gollum-editor-form");
  if (form) {
    form.addEventListener("submit", () => { dirty = false; });
  }

  return listener;
}

// --- Main init ---

function initEditor() {
  const textarea = document.getElementById("gollum-editor-body");
  if (!textarea) return;

  textarea.style.display = "none";

  const wrapper = document.createElement("div");
  wrapper.id = "cm-editor-wrapper";
  textarea.parentNode.insertBefore(wrapper, textarea);

  const unsavedListener = initUnsavedWarning();

  const view = new EditorView({
    doc: textarea.value,
    extensions: [
      lineNumbers(),
      history(),
      bracketMatching(),
      closeBrackets(),
      highlightSelectionMatches(),
      markdown({ base: markdownLanguage, codeLanguages: languages }),
      syntaxHighlighting(defaultHighlightStyle, { fallback: true }),
      EditorView.lineWrapping,
      placeholder("Write your content here..."),
      editorKeymap,
      keymap.of([
        ...closeBracketsKeymap,
        ...historyKeymap,
        ...searchKeymap,
        indentWithTab,
        ...defaultKeymap,
      ]),
      EditorView.updateListener.of((update) => {
        if (update.docChanged) {
          textarea.value = update.state.doc.toString();
        }
      }),
      unsavedListener,
    ],
    parent: wrapper,
  });

  initToolbar(view);
  initPreviewTab(view);
}

document.addEventListener("DOMContentLoaded", initEditor);
