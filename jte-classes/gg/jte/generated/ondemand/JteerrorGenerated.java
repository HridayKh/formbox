package gg.jte.generated.ondemand;
@SuppressWarnings("unchecked")
@javax.annotation.processing.Generated("gg.jte.TemplateEngine")
public final class JteerrorGenerated {
	public static final String JTE_NAME = "error.jte";
	public static final int[] JTE_LINE_INFO = {0,0,0,0,0,0,15,15,15,15,16,16,16,17,17,17,24,24,24,0,1,2,2,2,2};
	public static void render(gg.jte.html.HtmlTemplateOutput jteOutput, gg.jte.html.HtmlInterceptor jteHtmlInterceptor, String errorTitle, String errorMessage, org.springframework.http.HttpStatus errorStatus) {
		jteOutput.writeContent("\n<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n    <meta charset=\"UTF-8\">\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n    <title>Error</title>\n</head>\n<body>\n<div>\n    <div>\n        <div></div>\n        <h2>");
		jteOutput.setContext("h2", null);
		jteOutput.writeUserContent(errorTitle);
		jteOutput.writeContent("</h2>\n        <p>");
		jteOutput.setContext("p", null);
		jteOutput.writeUserContent(errorMessage);
		jteOutput.writeContent("</p>\n        <p>");
		jteOutput.setContext("p", null);
		jteOutput.writeUserContent(errorStatus);
		jteOutput.writeContent("</p>\n        <div>\n            <a href=\"/static\">Go to Main Page</a>\n        </div>\n    </div>\n</div>\n</body>\n</html>");
	}
	public static void renderMap(gg.jte.html.HtmlTemplateOutput jteOutput, gg.jte.html.HtmlInterceptor jteHtmlInterceptor, java.util.Map<String, Object> params) {
		String errorTitle = (String)params.get("errorTitle");
		String errorMessage = (String)params.get("errorMessage");
		org.springframework.http.HttpStatus errorStatus = (org.springframework.http.HttpStatus)params.get("errorStatus");
		render(jteOutput, jteHtmlInterceptor, errorTitle, errorMessage, errorStatus);
	}
}
