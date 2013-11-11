package edu.kit.scc.dei.ecplean;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class ECPAuthenticator extends ECPAuthenticatorBase {

    public ECPAuthenticator(DefaultHttpClient client, String username,
        String password, URI idpEcpEndpoint, URI spUrl)
    {
        super(client);

        authInfo = new ECPAuthenticationInfo(username, password,
            idpEcpEndpoint, spUrl);
        authInfo.setAuthState(ECPAuthState.NOT_STARTED);
    }

    public ECPAuthenticator(String username, String password,
        URI idpEcpEndpoint, URI spUrl)
    {
        this(new DefaultHttpClient(), username, password, idpEcpEndpoint, spUrl);
    }

    public String authenticate() throws ECPAuthenticationException {
        logger.info("Starting authentication");

        logger.info("Contacting SP " + authInfo.getSpUrl());
        authInfo.setAuthState(ECPAuthState.INITIAL_PAOS_SP);
        setChanged();
        notifyObservers(authInfo);

        logger.info("Sending initial SP Request");

        HttpGet httpGet = new HttpGet(authInfo.getSpUrl().toString());
        httpGet.setHeader("Accept", "text/html; application/vnd.paos+xml");
        httpGet
            .setHeader("PAOS",
                "ver='urn:liberty:paos:2003-08';'urn:oasis:names:tc:SAML:2.0:profiles:SSO:ecp'");

        HttpResponse httpResponse;
        String responseBody;
        try {
            httpResponse = client.execute(httpGet);
            responseBody = EntityUtils.toString(httpResponse.getEntity());
        } catch (ClientProtocolException e) {
            logger.debug("Initial SP Request failed");
            throw new ECPAuthenticationException(e);
        } catch (ParseException e) {
            logger.debug("Initial SP Request failed");
            throw new ECPAuthenticationException(e);
        } catch (IOException e) {
            logger.debug("Initial SP Request failed");
            throw new ECPAuthenticationException(e);
        }

        Document initResponse;
        try {
            initResponse = buildDocumentFromString(responseBody);
        } catch (IOException e) {
            logger.debug("Parsing SP Request failed");
            throw new ECPAuthenticationException(e);
        } catch (ParserConfigurationException e) {
            logger.debug("Parsing SP Request failed");
            throw new ECPAuthenticationException(e);
        } catch (SAXException e) {
            logger.debug("Parsing SP Request failed");
            throw new ECPAuthenticationException(e);
        }

        String relayState;
        try {
            relayState = (String) queryDocument(initResponse,
                "//ecp:RelayState", XPathConstants.STRING);
        } catch (XPathException e) {
            logger.debug("Could not find relay state in PAOS answer from SP");
            throw new ECPAuthenticationException(e);
        }
        logger.info("Got relayState: " + relayState);
        String responseConsumerUrl;
        try {
            responseConsumerUrl = (String) queryDocument(initResponse,
                "/S:Envelope/S:Header/paos:Request/@responseConsumerURL",
                XPathConstants.STRING);
        } catch (XPathException e) {
            logger
                .debug("Could not find response consumer url in PAOS answer from SP");
            throw new ECPAuthenticationException(e);
        }
        logger.info("Got responseConsumerUrl: " + responseConsumerUrl);

        Node firstChild = initResponse.getDocumentElement().getFirstChild();
        initResponse.getDocumentElement().removeChild(firstChild);

        Document idpResponse = authenticateIdP(initResponse);

        String assertionConsumerUrl;
        try {
            assertionConsumerUrl = (String) queryDocument(
                idpResponse,
                "/S:Envelope/S:Header/ecp:Response/@AssertionConsumerServiceURL",
                XPathConstants.STRING);
        } catch (XPathException e) {
            logger
                .debug("Could not find assertion consumer url in answer from IdP");
            throw new ECPAuthenticationException(e);
        }
        logger.info("Got assertionConsumerUrl: " + assertionConsumerUrl);

        if (!assertionConsumerUrl.equals(responseConsumerUrl)) {
            try {
                System.out.println(documentToString(idpResponse));
            } catch (TransformerConfigurationException e) {
                e.printStackTrace();
            } catch (TransformerException e) {
                e.printStackTrace();
            }
            throw new ECPAuthenticationException(
                "Assertion- and ResponseConsumerURL don't match. responseConsumeUrl="
                    + responseConsumerUrl + ", assertionConsumerUrl="
                    + assertionConsumerUrl);
        }

        try {
            System.out.println(documentToString(idpResponse));
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        idpResponse.getDocumentElement().getFirstChild().getFirstChild()
            .setTextContent(relayState);

        logger.info("Sending Assertion to SP");
        HttpPost httpPost = new HttpPost(assertionConsumerUrl);
        httpPost.setHeader("Content-Type", "application/vnd.paos+xml");
        try {
            httpPost.setEntity(new StringEntity(documentToString(idpResponse)));
            httpResponse = client.execute(httpPost);
            responseBody = EntityUtils.toString(httpResponse.getEntity());
        } catch (UnsupportedEncodingException e) {
            logger.debug("Could not post assertion back to SP");
            throw new ECPAuthenticationException(e);
        } catch (TransformerConfigurationException e) {
            logger.debug("Could not post assertion back to SP");
            throw new ECPAuthenticationException(e);
        } catch (ClientProtocolException e) {
            logger.debug("Could not post assertion back to SP");
            throw new ECPAuthenticationException(e);
        } catch (ParseException e) {
            logger.debug("Could not post assertion back to SP");
            throw new ECPAuthenticationException(e);
        } catch (TransformerException e) {
            logger.debug("Could not post assertion back to SP");
            throw new ECPAuthenticationException(e);
        } catch (IOException e) {
            logger.debug("Could not post assertion back to SP");
            throw new ECPAuthenticationException(e);
        }

        logger.info("Requesting original URL");
        httpGet = new HttpGet(authInfo.getSpUrl().toString());
        try {
            httpResponse = client.execute(httpGet);
            responseBody = EntityUtils.toString(httpResponse.getEntity());
            logger.info(responseBody);
            int i = responseBody.indexOf("urn:oid:0.9.2342.19200300.100.1.3");
            if (i < 0) {
                return null;
            }
            int x = responseBody.indexOf(':', i + 10);
            if (x < 0) {
                return null;
            }
            int e = responseBody.indexOf('\n', x);
            if (e < 0) {
                e = responseBody.indexOf("urn:", x);
                if (e < 0) {
                    e = responseBody.length();
                }
            }
            String email = responseBody.substring(x + 1, e).trim()
                .toLowerCase();
            return email;
        } catch (ClientProtocolException e) {
            logger.debug("Could not request original URL");
            throw new ECPAuthenticationException(e);
        } catch (ParseException e) {
            logger.debug("Could not request original URL");
            throw new ECPAuthenticationException(e);
        } catch (IOException e) {
            logger.debug("Could not request original URL");
            throw new ECPAuthenticationException(e);
        }

    }
}
