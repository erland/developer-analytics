package io.github.developeranalytics.service.report;

import io.github.developeranalytics.domain.report.CanonicalReport;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.*;

@ApplicationScoped
public class PdfReportRenderer {

    @Inject ReportSectionPlanner planner;

    public byte[] render(
            CanonicalReport report,
            MarkdownReportType reportType
    ) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {

            Layout layout = new Layout(document, report);
            layout.heading(reportType.title(), 20);
            layout.paragraph(report.summary().overview());

            for (ReportSection section : planner.sections(reportType)) {
                switch (section) {
                    case METADATA -> metadata(layout, report, reportType);
                    case DATA_COVERAGE -> coverage(layout, report);
                    case PROJECT_CATEGORIES -> categories(layout, report);
                    case TECHNOLOGY_ANALYSIS -> technologies(layout, report);
                    case ACTIVITY -> activity(layout, report);
                    case SIGNIFICANT_PROJECTS -> projects(layout, report);
                    case ROLE_AI_ASSESSMENT -> ai(layout, report);
                    case METHODOLOGY -> methodology(layout, report);
                }
            }

            layout.finish();
            document.save(bytes);
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to render PDF report", exception);
        }
    }

    private void metadata(Layout out, CanonicalReport report, MarkdownReportType type)
            throws IOException {
        out.heading("Report metadata", 14);
        out.keyValue("Report type", type.name());
        out.keyValue("Report model", report.modelVersion());
        out.keyValue("Generated", report.generatedAt().toString());
        out.keyValue("Privacy scope", report.privacyScope().name());
        out.keyValue(
                "Period",
                date(report.period().firstActivityAt()) + " - " +
                        date(report.period().lastActivityAt())
        );
    }

    private void coverage(Layout out, CanonicalReport report) throws IOException {
        out.heading("Data coverage", 14);
        out.keyValue("Repositories", report.dataCoverage().repositoryCount());
        out.keyValue("Public repositories", report.dataCoverage().publicRepositoryCount());
        out.keyValue("Private repositories represented", report.dataCoverage().privateRepositoryCount());
        out.keyValue("Repositories available as detail", report.dataCoverage().repositoriesIncludedInDetail());
        out.keyValue("Contributions", report.dataCoverage().contributionCount());
    }

    private void categories(Layout out, CanonicalReport report) throws IOException {
        out.heading("Project categories", 14);
        if (report.projectCategories().isEmpty()) {
            out.paragraph("No project-category analysis available.");
            return;
        }
        for (var item : report.projectCategories()) {
            out.compactRow(item.name(), item.projectCount() + " projects");
        }
    }

    private void technologies(Layout out, CanonicalReport report) throws IOException {
        out.heading("Technology analysis", 14);
        if (report.technologyAnalysis().isEmpty()) {
            out.paragraph("No technology analysis available.");
            return;
        }

        // Intentionally rendered as wrapping cards rather than a 7-column table.
        // This prevents wide-table clipping on A4.
        for (var item : report.technologyAnalysis()) {
            out.card(
                    item.name(),
                    List.of(
                            "Evidence: " + item.evidenceLevel() +
                                    " | score " + item.evidenceScore(),
                            "Projects: " + item.projectCount(),
                            "Observed: " + date(item.firstObservedAt()) +
                                    " - " + date(item.lastObservedAt()),
                            "Privacy: " + item.privacyProvenance()
                    )
            );
        }
    }

    private void activity(Layout out, CanonicalReport report) throws IOException {
        out.heading("Activity", 14);
        out.heading("Contribution totals", 11);
        for (var entry : report.activity().byType().entrySet()) {
            out.compactRow(entry.getKey(), Integer.toString(entry.getValue()));
        }

        out.heading("Monthly activity", 11);
        if (report.activity().monthly().isEmpty()) {
            out.paragraph("No monthly activity available.");
            return;
        }

        int max = report.activity().monthly().stream()
                .mapToInt(CanonicalReport.ActivityMonth::contributionCount)
                .max().orElse(1);

        // A compact print-native horizontal chart, independent of dashboard CSS.
        for (var month : report.activity().monthly()) {
            out.activityBar(
                    month.month(),
                    month.contributionCount(),
                    month.activeProjectCount(),
                    max
            );
        }
    }

    private void projects(Layout out, CanonicalReport report) throws IOException {
        out.heading("Significant projects", 14);
        if (report.significantProjects().isEmpty()) {
            out.paragraph("No significant projects available for this privacy scope.");
            return;
        }
        for (var project : report.significantProjects()) {
            out.card(
                    project.repositoryName(),
                    List.of(
                            "Visibility: " + project.visibility() +
                                    " | ownership: " + project.ownership(),
                            "Significance: " + project.significanceLevel() +
                                    " (" + project.significanceScore() + ")",
                            "Involvement: " + project.involvementLevel() +
                                    " (" + project.involvementScore() + ")"
                    )
            );
        }
    }

    private void ai(Layout out, CanonicalReport report) throws IOException {
        out.heading("Role / AI assessment", 14);
        if (!report.roleAiAssessment().available()) {
            out.paragraph("No AI-generated user-level assessment is included in this report.");
            return;
        }

        out.callout("AI-generated interpretation");
        out.paragraph("Technical focus: " + report.roleAiAssessment().technicalFocus());
        out.paragraph("Breadth/depth: " + report.roleAiAssessment().breadthDepthObservation());
        out.paragraph("Technology evolution: " + report.roleAiAssessment().technologyEvolutionSummary());
        out.paragraph("Open-source engagement: " + report.roleAiAssessment().openSourceEngagementSummary());

        for (var role : report.roleAiAssessment().likelyRoles()) {
            out.paragraph(
                    "Likely role: " + role.role() + " (" +
                            Math.round(role.confidence() * 100) +
                            "% confidence) - " + role.rationale()
            );
        }
    }

    private void methodology(Layout out, CanonicalReport report) throws IOException {
        out.heading("Methodology", 14);
        out.paragraph(report.methodology().measuredDataStatement());
        out.paragraph(report.methodology().inferenceStatement());
        out.paragraph(report.methodology().correctionStatement());

        if (!report.methodology().sourceTypes().isEmpty()) {
            out.heading("Source types", 11);
            for (String source : report.methodology().sourceTypes()) {
                out.paragraph("- " + source);
            }
        }
    }

    private String date(OffsetDateTime value) {
        return value == null ? "No recorded activity" : value.toLocalDate().toString();
    }

    private static final class Layout {
        private static final float MARGIN = 45;
        private static final float FOOTER = 32;
        private static final float BODY_SIZE = 9.5f;
        private static final float LEADING = 13f;

        private final PDDocument document;
        private final CanonicalReport report;
        private final PDFont regular =
                new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        private final PDFont bold =
                new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

        private PDPage page;
        private PDPageContentStream stream;
        private float y;
        private int pageNumber;

        Layout(PDDocument document, CanonicalReport report) throws IOException {
            this.document = document;
            this.report = report;
            newPage();
        }

        void finish() throws IOException {
            if (stream != null) {
                footer();
                stream.close();
            }
        }

        void heading(String text, float size) throws IOException {
            ensure(size + 18);
            y -= size >= 14 ? 8 : 4;
            textLine(text, bold, size, MARGIN, y);
            y -= size + 7;
        }

        void paragraph(String text) throws IOException {
            if (text == null || text.isBlank()) return;
            for (String line : wrap(text, regular, BODY_SIZE, width())) {
                ensure(LEADING);
                textLine(line, regular, BODY_SIZE, MARGIN, y);
                y -= LEADING;
            }
            y -= 5;
        }

        void keyValue(String key, Object value) throws IOException {
            String text = key + ": " + String.valueOf(value);
            for (String line : wrap(text, regular, BODY_SIZE, width())) {
                ensure(LEADING);
                textLine(line, regular, BODY_SIZE, MARGIN, y);
                y -= LEADING;
            }
        }

        void compactRow(String label, String value) throws IOException {
            ensure(18);
            textLine(label, regular, BODY_SIZE, MARGIN, y);
            float valueWidth = regular.getStringWidth(value) / 1000 * BODY_SIZE;
            textLine(value, bold, BODY_SIZE, page.getMediaBox().getWidth()-MARGIN-valueWidth, y);
            y -= 16;
        }

        void card(String title, List<String> lines) throws IOException {
            List<String> wrapped = new ArrayList<>();
            for (String line : lines) {
                wrapped.addAll(wrap(line, regular, BODY_SIZE, width()-18));
            }
            float h = 24 + wrapped.size()*LEADING + 8;
            ensure(h);
            float bottom = y-h+8;
            stream.addRect(MARGIN, bottom, width(), h);
            stream.setLineWidth(.4f);
            stream.stroke();
            textLine(title, bold, 10.5f, MARGIN+8, y-14);
            float cy=y-29;
            for (String line : wrapped) {
                textLine(line, regular, BODY_SIZE, MARGIN+8, cy);
                cy-=LEADING;
            }
            y=bottom-9;
        }

        void callout(String text) throws IOException {
            List<String> lines=wrap(text,bold,10,width()-18);
            float h=16+lines.size()*13;
            ensure(h);
            float bottom=y-h+5;
            stream.addRect(MARGIN,bottom,width(),h);
            stream.setLineWidth(1f);
            stream.stroke();
            float cy=y-14;
            for(String line:lines){
                textLine(line,bold,10,MARGIN+8,cy);
                cy-=13;
            }
            y=bottom-8;
        }

        void activityBar(
                String month,
                int contributions,
                int activeProjects,
                int max
        ) throws IOException {
            ensure(23);
            float labelWidth=72;
            float valueWidth=72;
            float barWidth=width()-labelWidth-valueWidth-12;
            float ratio=max<=0?0:(float)contributions/max;
            textLine(month, regular, 8.5f, MARGIN, y);
            float bx=MARGIN+labelWidth;
            stream.addRect(bx,y-2,barWidth,7);
            stream.setLineWidth(.25f);
            stream.stroke();
            if(contributions>0){
                stream.addRect(bx,y-2,Math.max(2,barWidth*ratio),7);
                stream.fill();
            }
            String value=contributions+" / "+activeProjects+" projects";
            textLine(value, regular, 7.5f, bx+barWidth+6, y);
            y-=18;
        }

        private void newPage() throws IOException {
            if (stream != null) {
                footer();
                stream.close();
            }
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document,page);
            pageNumber++;
            y=page.getMediaBox().getHeight()-MARGIN;

            // Persistent privacy marking on every page.
            String mark="Privacy: "+report.privacyScope().name();
            textLine(mark,bold,7.5f,MARGIN,y+13);
        }

        private void footer() throws IOException {
            String footer="Developer Analytics | "+report.modelVersion()+
                    " | "+report.privacyScope().name()+" | page "+pageNumber;
            textLine(footer,regular,7,MARGIN,FOOTER-5);
        }

        private void ensure(float needed) throws IOException {
            if(y-needed < FOOTER+16){
                newPage();
            }
        }

        private float width(){
            return page.getMediaBox().getWidth()-2*MARGIN;
        }

        private void textLine(
                String text,
                PDFont font,
                float size,
                float x,
                float baseline
        ) throws IOException {
            stream.beginText();
            stream.setFont(font,size);
            stream.newLineAtOffset(x,baseline);
            stream.showText(safe(text));
            stream.endText();
        }

        private List<String> wrap(
                String text,
                PDFont font,
                float size,
                float maxWidth
        ) throws IOException {
            String clean=safe(text);
            List<String> result=new ArrayList<>();
            for(String paragraph:clean.split("\\n",-1)){
                if(paragraph.isBlank()){
                    result.add("");
                    continue;
                }
                StringBuilder line=new StringBuilder();
                for(String word:paragraph.split("\\s+")){
                    String candidate=line.isEmpty()?word:line+" "+word;
                    float w=font.getStringWidth(candidate)/1000*size;
                    if(w<=maxWidth || line.isEmpty()){
                        line.setLength(0);
                        line.append(candidate);
                    }else{
                        result.add(line.toString());
                        line.setLength(0);
                        line.append(word);
                    }
                }
                if(!line.isEmpty()) result.add(line.toString());
            }
            return result;
        }

        private String safe(String value){
            if(value==null) return "";
            return value
                    .replace("–","-")
                    .replace("—","-")
                    .replace("’","'")
                    .replace("“","\"")
                    .replace("”","\"")
                    .replace("•","-");
        }
    }
}
