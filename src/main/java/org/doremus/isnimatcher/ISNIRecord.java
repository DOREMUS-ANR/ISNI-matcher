package org.doremus.isnimatcher;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "ISNIAssigned")
@XmlAccessorType(XmlAccessType.FIELD)
public class ISNIRecord {
  @XmlElement(name = "isniUnformatted")
  public String id;

  @XmlElement(name = "isniURI")
  public String uri;

  @XmlElement(name = "personalName")
  public List<PersonalName> personalNames;

  @XmlElement(name = "externalInformation")
  private List<ExternalInformation> externalInformations;

  @XmlElement(name = "source")
  private List<Source> sources1;

  @XmlElement(name = "sources")
  private List<Source> sources2;

  private List<Source> source = null;

  private String body;

  public List<Source> getSources() {
    if (source == null) {
      source = sources1;
      if (sources1 == null) source = sources2;
      else source.addAll(sources2);
    }
    return source;
  }

  public List<ExternalInformation> getExternalInformations() {
    if (externalInformations == null)
      externalInformations = new ArrayList<>();
    return externalInformations;
  }

  private void setBody(String body) {
    this.body = body;
  }

  public static ISNIRecord fromFile(String fileName) throws IOException, JAXBException {
    return fromFile(Paths.get(fileName));
  }

  public static ISNIRecord fromFile(Path file) throws IOException, JAXBException {
    String fileContent = new String(Files.readAllBytes(file));

    if (!fileContent.startsWith("<ISNIAssigned>"))
      fileContent = ISNI.splitBody(fileContent).get(0);

    return fromString(fileContent);
  }

  public static ISNIRecord fromUri(URL uri) throws JAXBException {
    JAXBContext jaxbContext = JAXBContext.newInstance(ISNIRecord.class);
    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    return (ISNIRecord) jaxbUnmarshaller.unmarshal(uri);
  }


  public static ISNIRecord fromUri(String uri) throws MalformedURLException, JAXBException {
    return fromUri(new URL(uri));
  }

  public static ISNIRecord fromString(String record) throws JAXBException {
    JAXBContext jaxbContext = JAXBContext.newInstance(ISNIRecord.class);
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

    // remove useless intermediate tags
    record = record.replaceAll("</?ISNIMetadata>", "");
    record = record.replaceAll("</?identity>", "");
    record = record.replaceAll("</?personOrFiction>", "");

    StringReader reader = new StringReader(record);
    ISNIRecord rec = (ISNIRecord) unmarshaller.unmarshal(reader);
    rec.setBody(record);
    return rec;
  }


  public String getViafURI() {
    for (ExternalInformation ex : getExternalInformations())
      if (ex.isType("viaf")) return ex.URI;

    for (Source s : getSources())
      if ("VIAF".equals(s.codeOfSource)) return s.asViafURI();

    return null;
  }

  public String getDiscogsURI() {
    for (ExternalInformation ex : getExternalInformations())
      if (ex.isType("discogs")) return ex.URI;
    return null;
  }

  public String getWikidataURI() {
    for (ExternalInformation ex : getExternalInformations())
      if (ex.isType("wikidata")) return ex.URI;
    return null;
  }

  public String getMuziekwebURI() {
    for (ExternalInformation ex : getExternalInformations())
      if (ex.isType("muziekweb")) return ex.URI;
    return null;
  }

  private ExternalInformation getWikipedia(String lang) {
    for (ExternalInformation ex : getExternalInformations()) {
      if (ex.isType("Wikipedia") && lang.equalsIgnoreCase(ex.getLang()))
        return ex;
    }
    return null;
  }

  public String getWikipediaUri() {
    return getWikipediaUri("en");
  }

  public String getWikipediaUri(String lang) {
    ExternalInformation wk = getWikipedia(lang);
    if (wk == null) return null;
    return wk.URI;
  }

  public String getDBpediaUri(String lang) {
    ExternalInformation wk = getWikipedia(lang);
    if (wk == null) return null;
    return wk.toDBpedia();
  }

  public String getDBpediaUri() {
    return getDBpediaUri("en");
  }

  public String getMusicBrainzUri() {
    for (Source s : getSources()) {
      if ("MUBZ".equals(s.codeOfSource))
        return s.asMusicBrainzURI();
    }
    return null;
  }

  public String getBNFUri() {
    for (Source s : getSources()) {
      if ("BNF".equals(s.codeOfSource))
        return s.asBNFUri();
    }
    return null;
  }


  public String getBirthYear() {
    for (PersonalName n : personalNames) {
      String by = n.getBirthYear();
      if (by != null) return by;
    }
    return null;
  }

  public String getDeathYear() {
    for (PersonalName n : personalNames) {
      String dy = n.getDeathYear();
      if (dy != null) return dy;
    }
    return null;
  }

  public void save(String destination) throws IOException {
    this.save(Paths.get(destination));
  }

  public void save(Path destination) throws IOException {
    byte[] strToBytes = this.body.getBytes();
    Files.write(destination, strToBytes);
  }
}
