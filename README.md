# KnowledgeBase

A browser-based knowledge base and markdown editor with live preview, diagrams, math, AI assistance, and Excel integration. Single HTML file, zero server required.
Try it ! 

## Features

### Markdown Editor
- Live split-pane preview (editor + rendered output)
- Toolbar with formatting buttons (bold, italic, headings, lists, tables, blockquotes, code)
- Find & replace (Ctrl+F)
- Tab bar for multiple open files
- Auto-save with visual indicator
- Outline panel for document navigation
- Word count and line indicator

### Rich Content Support
- **Mermaid diagrams** — flowcharts, sequence diagrams, Gantt charts, and more
- **KaTeX math** — LaTeX formula rendering inline and display mode
- **SVG drawings** — import and render SVG with DSL blocks
- **DXF import** — AutoCAD drawings converted to SVG display
- **iFrame embeds** — embed external content
- **Calculator blocks** — inline spreadsheet-style calculations with formulas
- **Excel tables** — sortable, searchable data tables from JSON

### Excel Integration
- **XLS → JSON import** — VBA macro generates JSON from Excel selections, paste into KnowledgeBase
- Auto-detect column types (numbers, text)
- Sortable columns, search/filter
- Formula bar display

### AI Assistant
- Powered by Groq API (free tier)
- Multiple modes: Write, Continue, Improve, Summarise, Diagram, SVG Drawing, Table, Translate, Explain
- Streaming output with stop control
- Insert at cursor, replace selection, or append to document
- Configurable models (Llama 3.3, Mixtral, Gemma, DeepSeek)

### File Management
- Open local folder via File System Access API
- Sidebar tree with folders and files
- Create, rename, move, delete files and folders
- Full-text search across all notes
- Browser storage fallback (no folder needed)

### Interface
- Dark / Light theme toggle
- Resizable sidebar
- Resizable editor/preview splitter
- Keyboard shortcuts (Ctrl+B/I/E/N/F, etc.)
- Toast notifications
- Export to DOC or raw Markdown

## Getting Started

Open `kb_improved_SVG.html` in any modern web browser (Chrome/Edge recommended for folder access).

## License

MIT
