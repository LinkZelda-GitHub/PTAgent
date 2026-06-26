package com.ptagent.common;

import com.ptagent.exception.ApiException;
import com.ptagent.exception.ErrorCode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class XlsxReader {
    private static final String REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";

    private XlsxReader() {
    }

    public static List<Sheet> read(Path path) {
        try (ZipFile zip = new ZipFile(path.toFile())) {
            Document workbook = xml(zip, "xl/workbook.xml");
            Document rels = xml(zip, "xl/_rels/workbook.xml.rels");
            List<String> sharedStrings = sharedStrings(zip);
            Map<String, String> relationships = relationships(rels);
            List<Sheet> sheets = new ArrayList<>();
            for (Element sheetNode : elements(workbook, "sheet")) {
                String name = sheetNode.getAttribute("name");
                String relId = sheetNode.getAttributeNS(REL_NS, "id");
                String target = relationships.get(relId);
                if (target == null || target.isBlank()) {
                    continue;
                }
                String sheetPath = target.startsWith("/") ? target.substring(1) : "xl/" + target;
                Document sheetXml = xml(zip, sheetPath);
                sheets.add(new Sheet(name, rows(sheetXml, sharedStrings)));
            }
            return sheets;
        } catch (IOException e) {
            throw ApiException.badRequest(ErrorCode.IMPORT_INVALID_FILE, "XLSX文件无法读取");
        }
    }

    private static List<List<String>> rows(Document sheetXml, List<String> sharedStrings) {
        List<List<String>> rows = new ArrayList<>();
        for (Element rowNode : elements(sheetXml, "row")) {
            List<String> row = new ArrayList<>();
            for (Element cellNode : childElements(rowNode, "c")) {
                int column = columnIndex(cellNode.getAttribute("r"));
                while (row.size() < column) {
                    row.add("");
                }
                row.add(cellValue(cellNode, sharedStrings));
            }
            rows.add(row);
        }
        return rows;
    }

    private static String cellValue(Element cell, List<String> sharedStrings) {
        String type = cell.getAttribute("t");
        if ("inlineStr".equals(type)) {
            return text(cell, "t");
        }
        String value = text(cell, "v");
        if ("s".equals(type) && !value.isBlank()) {
            int index = Integer.parseInt(value);
            return index >= 0 && index < sharedStrings.size() ? sharedStrings.get(index) : "";
        }
        return value;
    }

    private static int columnIndex(String cellRef) {
        int index = 0;
        for (int i = 0; i < cellRef.length(); i++) {
            char ch = cellRef.charAt(i);
            if (ch < 'A' || ch > 'Z') {
                break;
            }
            index = index * 26 + (ch - 'A' + 1);
        }
        return Math.max(0, index - 1);
    }

    private static List<String> sharedStrings(ZipFile zip) throws IOException {
        if (zip.getEntry("xl/sharedStrings.xml") == null) {
            return List.of();
        }
        Document sharedXml = xml(zip, "xl/sharedStrings.xml");
        List<String> values = new ArrayList<>();
        for (Element si : elements(sharedXml, "si")) {
            values.add(text(si, "t"));
        }
        return values;
    }

    private static Map<String, String> relationships(Document rels) {
        Map<String, String> map = new HashMap<>();
        for (Element rel : elements(rels, "Relationship")) {
            map.put(rel.getAttribute("Id"), rel.getAttribute("Target"));
        }
        return map;
    }

    private static String text(Element element, String tagName) {
        StringBuilder text = new StringBuilder();
        for (Element item : elements(element, tagName)) {
            text.append(item.getTextContent());
        }
        return text.toString();
    }

    private static List<Element> elements(Document document, String localName) {
        return elements(document.getDocumentElement(), localName);
    }

    private static List<Element> elements(Element root, String localName) {
        List<Element> elements = new ArrayList<>();
        NodeList nodes = root.getElementsByTagNameNS("*", localName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element element) {
                elements.add(element);
            }
        }
        return elements;
    }

    private static List<Element> childElements(Element root, String localName) {
        List<Element> elements = new ArrayList<>();
        NodeList nodes = root.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element element && localName.equals(element.getLocalName())) {
                elements.add(element);
            }
        }
        return elements;
    }

    private static Document xml(ZipFile zip, String entryName) throws IOException {
        ZipEntry entry = zip.getEntry(entryName);
        if (entry == null) {
            throw ApiException.badRequest(ErrorCode.IMPORT_INVALID_FILE, "XLSX结构不完整：" + entryName);
        }
        try (InputStream input = zip.getInputStream(entry)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(input);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw ApiException.badRequest(ErrorCode.IMPORT_INVALID_FILE, "XLSX XML解析失败：" + entryName);
        }
    }

    public record Sheet(String name, List<List<String>> rows) {
    }
}
