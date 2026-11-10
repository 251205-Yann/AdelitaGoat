"""Generate the school handout from docs/informe-tecnico.md."""

from __future__ import annotations

import html
import re
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    HRFlowable,
    KeepTogether,
    Paragraph,
    SimpleDocTemplate,
    Spacer,
    Table,
    TableStyle,
)


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "docs" / "informe-tecnico.md"
OUTPUT = ROOT / "output" / "pdf" / "Informe_tecnico_AdelitaGOAT.pdf"


def inline(value: str) -> str:
    value = html.escape(value.strip())
    value = re.sub(r"\[([^]]+)\]\(([^)]+)\)", r'<link href="\2" color="#225a88">\1</link>', value)
    value = re.sub(r"\*\*([^*]+)\*\*", r"<b>\1</b>", value)
    value = re.sub(r"`([^`]+)`", r'<font color="#624a2e">\1</font>', value)
    return value


def footer(canvas, doc):
    canvas.saveState()
    width, _ = A4
    canvas.setStrokeColor(colors.HexColor("#d5dee5"))
    canvas.line(18 * mm, 17 * mm, width - 18 * mm, 17 * mm)
    canvas.setFont("Arial", 8)
    canvas.setFillColor(colors.HexColor("#64717d"))
    canvas.drawString(18 * mm, 12 * mm, "AdelitaGOAT  |  Informe técnico escolar")
    canvas.drawRightString(width - 18 * mm, 12 * mm, str(doc.page))
    canvas.restoreState()


def main() -> None:
    pdfmetrics.registerFont(TTFont("Arial", "C:/Windows/Fonts/arial.ttf"))
    pdfmetrics.registerFont(TTFont("Arial-Bold", "C:/Windows/Fonts/arialbd.ttf"))
    pdfmetrics.registerFontFamily("Arial", normal="Arial", bold="Arial-Bold")

    base = getSampleStyleSheet()
    styles = {
        "title": ParagraphStyle("TitleCustom", parent=base["Title"], fontName="Arial-Bold", fontSize=22, leading=26, textColor=colors.HexColor("#163a53"), spaceAfter=8 * mm, alignment=TA_LEFT),
        "h2": ParagraphStyle("HeadingCustom", parent=base["Heading2"], fontName="Arial-Bold", fontSize=12, leading=15, textColor=colors.HexColor("#1d506e"), spaceBefore=6 * mm, spaceAfter=2.5 * mm, keepWithNext=True),
        "body": ParagraphStyle("BodyCustom", parent=base["BodyText"], fontName="Arial", fontSize=9.3, leading=13.5, textColor=colors.HexColor("#263744"), spaceAfter=3.1 * mm),
        "bullet": ParagraphStyle("BulletCustom", parent=base["BodyText"], fontName="Arial", fontSize=9.1, leading=13, leftIndent=5 * mm, firstLineIndent=-3 * mm, spaceAfter=1.7 * mm),
        "meta": ParagraphStyle("MetaCustom", parent=base["BodyText"], fontName="Arial", fontSize=9, leading=13, textColor=colors.HexColor("#4b5e6b"), spaceAfter=1 * mm),
        "cell": ParagraphStyle("CellCustom", parent=base["BodyText"], fontName="Arial", fontSize=8, leading=11, textColor=colors.HexColor("#263744")),
        "cellhead": ParagraphStyle("CellHeadCustom", parent=base["BodyText"], fontName="Arial-Bold", fontSize=8, leading=11, textColor=colors.white),
    }

    lines = SOURCE.read_text(encoding="utf-8").splitlines()
    story = []
    i = 0
    while i < len(lines):
        line = lines[i].strip()
        if not line:
            i += 1
            continue
        if line.startswith("# "):
            story.append(Paragraph(inline(line[2:]), styles["title"]))
            story.append(HRFlowable(width="100%", thickness=1.4, color=colors.HexColor("#d99436"), spaceAfter=6 * mm))
        elif line.startswith("## "):
            story.append(Paragraph(inline(line[3:]), styles["h2"]))
        elif line.startswith("|"):
            rows = []
            while i < len(lines) and lines[i].strip().startswith("|"):
                cells = [x.strip() for x in lines[i].strip().strip("|").split("|")]
                if not all(re.fullmatch(r":?-{2,}:?", x) for x in cells):
                    rows.append(cells)
                i += 1
            i -= 1
            col_count = len(rows[0])
            available = A4[0] - 36 * mm
            widths = [available * (0.25 if j == 0 else 0.75 / (col_count - 1)) for j in range(col_count)]
            data = [[Paragraph(inline(cell), styles["cellhead"] if row_index == 0 else styles["cell"]) for cell in row] for row_index, row in enumerate(rows)]
            table = Table(data, colWidths=widths, repeatRows=1, hAlign="LEFT")
            table.setStyle(TableStyle([
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#245271")),
                ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#f1f5f7")]),
                ("GRID", (0, 0), (-1, -1), 0.35, colors.HexColor("#d9e2e7")),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 7),
                ("RIGHTPADDING", (0, 0), (-1, -1), 7),
                ("TOPPADDING", (0, 0), (-1, -1), 6),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
            ]))
            story.extend([table, Spacer(1, 3 * mm)])
        elif line.startswith("- "):
            story.append(Paragraph("• " + inline(line[2:]), styles["bullet"]))
        elif line.startswith(("**Proyecto escolar:**", "**Versión del informe:**", "**Estado:**")):
            story.append(Paragraph(inline(line), styles["meta"]))
        else:
            paragraph = line.rstrip("  ")
            while i + 1 < len(lines) and lines[i + 1].strip() and not lines[i + 1].startswith(("#", "|", "- ")):
                i += 1
                paragraph += " " + lines[i].strip()
            story.append(Paragraph(inline(paragraph), styles["body"]))
        i += 1

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    doc = SimpleDocTemplate(str(OUTPUT), pagesize=A4, rightMargin=18 * mm, leftMargin=18 * mm, topMargin=17 * mm, bottomMargin=23 * mm, title="Informe técnico AdelitaGOAT", author="Equipo AdelitaGOAT")
    doc.build(story, onFirstPage=footer, onLaterPages=footer)
    print(OUTPUT)


if __name__ == "__main__":
    main()
