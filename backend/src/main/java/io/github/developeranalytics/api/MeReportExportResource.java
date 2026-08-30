package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.service.report.ReportExportService;
import io.github.developeranalytics.service.report.MarkdownReportType;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/me/reports")
@Consumes(MediaType.APPLICATION_JSON)
public class MeReportExportResource {

    @Inject
    CurrentUserService currentUserService;

    @Inject
    ReportExportService exports;

    @POST
    @Path("/preview")
    @Produces(MediaType.APPLICATION_JSON)
    public ReportExportService.PreviewResult preview(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String token,
            PreviewRequest request
    ) {
        CurrentUser current =
                currentUserService.requireCurrentUser(token);

        validateSettings(request);

        return exports.preview(
                current.user().getId(),
                request.reportType(),
                request.privateDataMode(),
                request.hidePrivateRepositoryNames()
        );
    }

    @POST
    @Path("/export")
    @Produces(MediaType.TEXT_PLAIN)
    public Response export(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String token,
            ExportRequest request
    ) {
        CurrentUser current =
                currentUserService.requireCurrentUser(token);

        if (request == null || !Boolean.TRUE.equals(
                request.generationConfirmed()
        )) {
            throw new BadRequestException(
                    "Report generation must be explicitly confirmed after preview"
            );
        }

        validateSettings(request);

        if (request.outputFormat() == OutputFormat.PDF) {
            ReportExportService.PdfExportResult result =
                    exports.exportPdf(
                            current.user().getId(),
                            request.reportType(),
                            request.privateDataMode(),
                            request.hidePrivateRepositoryNames()
                    );

            return Response.ok(result.pdf(), "application/pdf")
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" +
                                    result.filename() + "\""
                    )
                    .header(
                            "X-Private-Data-Mode",
                            result.privateDataMode().name()
                    )
                    .header(
                            "X-Hide-Private-Repository-Names",
                            Boolean.toString(
                                    result.hidePrivateRepositoryNames()
                            )
                    )
                    .header(
                            "X-Report-Model-Version",
                            result.reportModelVersion()
                    )
                    .header(
                            "X-Report-Type",
                            result.reportType().name()
                    )
                    .header("X-Report-Format", "PDF")
                    .build();
        }

        ReportExportService.ExportResult result =
                exports.exportMarkdown(
                        current.user().getId(),
                        request.reportType(),
                        request.privateDataMode(),
                        request.hidePrivateRepositoryNames()
                );

        return Response.ok(
                        result.markdown(),
                        "text/markdown; charset=utf-8"
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" +
                                result.filename() + "\""
                )
                .header(
                        "X-Private-Data-Mode",
                        result.privateDataMode().name()
                )
                .header(
                        "X-Hide-Private-Repository-Names",
                        Boolean.toString(
                                result.hidePrivateRepositoryNames()
                        )
                )
                .header(
                        "X-Report-Model-Version",
                        result.reportModelVersion()
                )
                .header(
                        "X-Report-Type",
                        result.reportType().name()
                )
                .header("X-Report-Format", "MARKDOWN")
                .build();
    }

    private void validateSettings(PreviewRequest request) {
        if (request == null ||
                request.outputFormat() == null ||
                request.reportType() == null ||
                request.privateDataMode() == null ||
                request.hidePrivateRepositoryNames() == null) {
            throw new BadRequestException(
                    "Report type and export privacy settings must be explicitly supplied"
            );
        }
    }

    public enum OutputFormat {
        MARKDOWN,
        PDF
    }

    public record PreviewRequest(
            OutputFormat outputFormat,
            MarkdownReportType reportType,
            ReportExportService.PrivateDataMode privateDataMode,
            Boolean hidePrivateRepositoryNames
    ) {}

    public record ExportRequest(
            OutputFormat outputFormat,
            MarkdownReportType reportType,
            ReportExportService.PrivateDataMode privateDataMode,
            Boolean hidePrivateRepositoryNames,
            Boolean generationConfirmed
    ) {}

    private void validateSettings(ExportRequest request) {
        if (request == null ||
                request.outputFormat() == null ||
                request.reportType() == null ||
                request.privateDataMode() == null ||
                request.hidePrivateRepositoryNames() == null) {
            throw new BadRequestException(
                    "Report type and export privacy settings must be explicitly supplied"
            );
        }
    }
}
