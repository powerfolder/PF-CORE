package edu.kit.scc.dei.ecplean;

import java.io.IOException;
import java.net.URI;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import de.dal33t.powerfolder.util.StringUtils;

public class ECPAuthenticator extends ECPAuthenticatorBase {

    public ECPAuthenticator(DefaultHttpClient client, String username,
        String password, URI idpEcpEndpoint, URI spUrl)
    {
        super(client);

        authInfo = new ECPAuthenticationInfo(username, password,
            idpEcpEndpoint, spUrl);
        authInfo.setAuthState(ECPAuthState.NOT_STARTED);
    }

    ECPAuthenticator(String username, String password,
        URI idpEcpEndpoint, URI spUrl)
    {
        this(new DefaultHttpClient(), username, password, idpEcpEndpoint, spUrl);
    }

    /**
     * @return an array containing: username and temporary security token to
     *         authenticate a client.
     * @throws ECPAuthenticationException
     */
    public String[] authenticate() throws ECPAuthenticationException {
        if (isInfo()) {
            LOG.info("Starting authentication. Contacting SP "
                + authInfo.getSpUrl());
        }

        authInfo.setAuthState(ECPAuthState.INITIAL_PAOS_SP);
        setChanged();
        notifyObservers(authInfo);

        if (isFine()) {
            LOG.fine("Sending initial SP Request");
        }

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
        } catch (IOException | ParseException e) {
            LOG.warning("Initial SP Request failed. " + e);
            throw new ECPAuthenticationException(e);
        }

        Document initResponse;
        try {
            initResponse = buildDocumentFromString(responseBody);
        } catch (IOException | SAXException | ParserConfigurationException e) {
            LOG.warning("Parsing SP Request failed. " + e);
            throw new ECPAuthenticationException(e);
        }

        String relayState;
        try {
            relayState = (String) queryDocument(initResponse,
                "//ecp:RelayState", XPathConstants.STRING);
        } catch (XPathException e) {
            LOG.warning("Could not find relay state in PAOS answer from SP. "
                + e);
            throw new ECPAuthenticationException(e);
        }
        if (isFine()) {
            LOG.fine("Got relayState: " + relayState);
        }
        String responseConsumerUrl;
        try {
            responseConsumerUrl = (String) queryDocument(initResponse,
                "/S:Envelope/S:Header/paos:Request/@responseConsumerURL",
                XPathConstants.STRING);
        } catch (XPathException e) {
            LOG.warning("Could not find response consumer url in PAOS answer from SP. "
                + e);
            throw new ECPAuthenticationException(e);
        }
        if (isFine()) {
            LOG.fine("Got responseConsumerUrl: " + responseConsumerUrl);
        }

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
            LOG.warning("Could not find assertion consumer url in answer from IdP. "
                + e);
            throw new ECPAuthenticationException(e);
        }
        if (isFine()) {
            LOG.fine("Got assertionConsumerUrl: " + assertionConsumerUrl);
        }

        if (!assertionConsumerUrl.equals(responseConsumerUrl)) {
            throw new ECPAuthenticationException(
                "Assertion- and ResponseConsumerURL don't match. responseConsumeUrl="
                    + responseConsumerUrl + ", assertionConsumerUrl="
                    + assertionConsumerUrl);
        }
        //
        // try {
        // System.out.println(documentToString(idpResponse));
        // } catch (TransformerConfigurationException e) {
        // e.printStackTrace();
        // } catch (TransformerException e) {
        // e.printStackTrace();
        // }

        idpResponse.getDocumentElement().getFirstChild().getFirstChild()
            .setTextContent(relayState);

        if (isFine()) {
            LOG.fine("Sending Assertion to SP");
        }
        HttpPost httpPost = new HttpPost(assertionConsumerUrl);
        httpPost.setHeader("Content-Type", "application/vnd.paos+xml");
        try {
            httpPost.setEntity(new StringEntity(documentToString(idpResponse)));
            httpResponse = client.execute(httpPost);
            responseBody = EntityUtils.toString(httpResponse.getEntity());
        } catch (IOException | TransformerException | ParseException e) {
            LOG.warning("Could not post assertion back to SP. " + e);
            throw new ECPAuthenticationException(e);
        }

        if (isInfo()) {
            LOG.info("Requesting original URL: " + authInfo.getSpUrl());
        }
        httpGet = new HttpGet(authInfo.getSpUrl().toString());
        try {
            httpResponse = client.execute(httpGet);
            int statusCode = httpResponse.getStatusLine().getStatusCode();

            if (statusCode == 403 || statusCode == 401) {
                throw new ECPUnauthorizedException(statusCode + " - "
                    + httpResponse.getStatusLine().getReasonPhrase());
            } else if (statusCode != 200) {
                throw new ECPUnauthorizedException(statusCode + " - "
                    + httpResponse.getStatusLine().getReasonPhrase());
            }

            responseBody = EntityUtils.toString(httpResponse.getEntity());

            if (isFine()) {
                LOG.fine("Got the following response from SP URL: "
                    + authInfo.getSpUrl() + ":\n" + responseBody);
            }

            JSONObject jsonObj = new JSONObject(responseBody);
            if (!jsonObj.has("shibboleth")) {
                throw new ECPAuthenticationException(
                    "Shibboleth login data not found in JSON response");
            }
            JSONObject shibObj = jsonObj.getJSONObject("shibboleth");
            String sessionID = shibObj.getString("sessionID");
            String persistentID = shibObj.getString("persistentID");
            String eppn = shibObj.getString("eppn");
            String email = shibObj.getString("email");
            String username = shibObj.getString("username");
            String token = shibObj.getString("token");

            if (isInfo()) {
                LOG.info("Shibboleth-Session-ID: " + sessionID);
                LOG.info("Shibboleth-Persistent-ID: " + persistentID);
                LOG.info("Shibboleth-EPPN: " + eppn);
                LOG.info("Shibboleth-Email: " + email);
                LOG.info("Shibboleth-Token: " + token);
            }

            if (StringUtils.isBlank(sessionID)) {
                throw new ECPUnauthorizedException(
                    "Invalid Shibboleth session ID");
            }

            return new String[]{username, token};
        } catch (IOException | ParseException | JSONException e) {
            LOG.warning("Could not request original URL: "
                + authInfo.getSpUrl() + " . " + e);
            throw new ECPAuthenticationException(e);
        }
    }
}
