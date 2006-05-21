package de.dal33t.powerfolder.web;

import java.io.StringWriter;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;

public abstract class AbstractVeloHandler extends PFComponent implements Handler {

    
    VelocityContext context;

    public AbstractVeloHandler(Controller controller) {
        super(controller);
    }
    
    public abstract String getTemplateFilename();

    public abstract void doRequest(HTTPRequest httpRequest);

    public HTTPResponse getPage(HTTPRequest httpRequest) {

        /* lets make a Context and put data into it */
        context = new VelocityContext();
        /* put the globoal vars in the context */
        context.put("PowerFolderVersion", Controller.PROGRAM_VERSION);
        context.put("velocityTools", VelocityTools.getInstance());
        doRequest(httpRequest);

        /* lets render a template */
        StringWriter writer = new StringWriter();
        try {
            Velocity.mergeTemplate(getTemplateFilename(),
                Velocity.ENCODING_DEFAULT, context, writer);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        HTTPResponse response = new HTTPResponse(writer.toString().getBytes());
        String mime = HTTPResponse.getMimeType(getTemplateFilename());
        if (mime != null) {
            response.setContentType(mime);
        }
        return response;
    }
}
