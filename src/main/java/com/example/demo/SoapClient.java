package com.example.demo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.SoapMessage;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.OutputStream;
import java.io.StringReader;

@Service
public class SoapClient {

    @Autowired
    private WebServiceTemplate webServiceTemplate;

    private static final String SOAP_ACTION = "http://tempuri.org/Add";

    public String callAddOperation(int intA, int intB) {
        String request =
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                        "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:temp=\"http://tempuri.org/\">" +
                        "<soap:Body>" +
                        "<temp:Add>" +
                        "<temp:intA>" + intA + "</temp:intA>" +
                        "<temp:intB>" + intB + "</temp:intB>" +
                        "</temp:Add>" +
                        "</soap:Body>" +
                        "</soap:Envelope>";

        return (String) webServiceTemplate.marshalSendAndReceive(request, message -> {
            SoapMessage soapMessage = (SoapMessage) message;
            soapMessage.setSoapAction(SOAP_ACTION);
            try {
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.transform(new StreamSource(new StringReader(request)), new StreamResult((OutputStream) soapMessage.getPayloadResult()));
            } catch (TransformerException e) {
                throw new RuntimeException("Error transforming SOAP request", e);
            }
        });
    }
}