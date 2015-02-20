package edu.kit.scc.dei.ecplean;

import java.io.IOException;
import java.net.URI;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

import org.apache.http.impl.client.HttpClientBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class ECPIdPAuth extends ECPAuthenticatorBase {

    public ECPIdPAuth(String username, String password, URI idpEcpEndpoint) {
        this(HttpClientBuilder.create(), username, password, idpEcpEndpoint);
    }

    public ECPIdPAuth(HttpClientBuilder clientBuilder, String username,
        String password, URI idpEcpEndpoint)
    {
        super(clientBuilder);

        authInfo = new ECPAuthenticationInfo(username, password,
            idpEcpEndpoint, null, null, null);
        authInfo.setAuthState(ECPAuthState.NOT_STARTED);
    }

    public String authenticate(String paosMessage)
        throws ECPAuthenticationException
    {

        Document initResponse;
        try {
            initResponse = buildDocumentFromString(paosMessage);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            LOG.fine("Parsing SP Request failed");
            throw new ECPAuthenticationException(e);
        }

        String relayState;
        try {
            relayState = (String) queryDocument(initResponse,
                "//ecp:RelayState", XPathConstants.STRING);
        } catch (XPathException e) {
            LOG.fine("Could not find relay state in PAOS answer from SP");
            throw new ECPAuthenticationException(e);
        }
        LOG.info("Got relayState: " + relayState);
        String responseConsumerUrl;
        try {
            responseConsumerUrl = (String) queryDocument(initResponse,
                "/S:Envelope/S:Header/paos:Request/@responseConsumerURL",
                XPathConstants.STRING);
        } catch (XPathException e) {
            LOG.fine("Could not find response consumer url in PAOS answer from SP");
            throw new ECPAuthenticationException(e);
        }
        LOG.info("Got responseConsumerUrl: " + responseConsumerUrl);

        Node firstChild = initResponse.getDocumentElement().getFirstChild();
        initResponse.getDocumentElement().removeChild(firstChild);

        Document idpResponse = authenticateIdP(initResponse);
        idpResponse.getDocumentElement().getFirstChild().getFirstChild()
            .setTextContent(relayState);

        try {
            return documentToString(idpResponse);
        } catch (TransformerException e) {
            LOG.fine("documentToString failed");
            throw new ECPAuthenticationException(e);
        }
    }

}
