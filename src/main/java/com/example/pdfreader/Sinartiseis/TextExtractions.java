package com.example.pdfreader.Sinartiseis;

import com.example.pdfreader.Entities.Document;
import com.example.pdfreader.HelloController;
import com.example.pdfreader.Helpers.ListManager;
import com.example.pdfreader.MyCustomEvents.TracingFolderEvent;
import com.example.pdfreader.TypesOfDocuments.*;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class TextExtractions {
    public static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy");

    public static String calculatePDFChecksum(File file){
        try {
            PDDocument document = PDDocument.load(file);
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String content = pdfStripper.getText(document);
            document.close();
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(content.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            System.out.println();
            System.err.println("File" + file.getName()+" Error processing : " + e.getMessage());
        }
        return null;
    }

    public static String calculateChecksum(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            FileInputStream fis = new FileInputStream(file);
            byte[] byteArray = new byte[1024];
            int bytesCount = 0;

            // Read the file data and update it to the message digest
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
            fis.close();

            // Convert the byte array to hex format
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }





    public static String removeLastLine(String content) {
        String[] lines = content.split("\n");
        if (lines.length <= 1) {
            return ""; // or return the original input if you don't want to return an empty string when there's only one line
        }
        String[] newLines = Arrays.copyOfRange(lines,0, lines.length - 1);
        return String.join("\n", newLines);
    }


    public static String removeStartingEmptyLines(String input) {
        int index = 0;
        while (index < input.length() && (input.charAt(index) == '\n' || input.charAt(index) == ' ')) {
            index++;
        }
        return input.substring(index);
    }

    public static String removeEmptyLines(String input){
        // Split the input text into lines
        String[] lines = input.split("\\r?\\n");
        // Filter out empty lines
        StringBuilder nonEmptyLines = new StringBuilder();
        for (String line : lines) {
            if (!line.trim().isEmpty()) { // Check if the line is not empty after trimming whitespace
                nonEmptyLines.append(line.trim()).append("\n"); // Append non-empty lines
            }
        }
        // Remove the trailing newline character, if needed
        if (!nonEmptyLines.isEmpty()) {
            nonEmptyLines.deleteCharAt(nonEmptyLines.length() - 1);
        }
        return nonEmptyLines.toString();
    }
    public static String removeLeadingEmptyLines(String str) {
        return str.replaceFirst("^(\\s*\\n)+", "");
    }


    public static void printPDFMetadata(File file) {
        try {
            PDDocument document = PDDocument.load(file);
            PDDocumentInformation info = document.getDocumentInformation();

            System.out.println("Title: " + info.getTitle());
            System.out.println("Author: " + info.getAuthor());
            System.out.println("Subject: " + info.getSubject());
            System.out.println("Creator: " + info.getCreator());
            System.out.println("Producer: " + info.getProducer());
            System.out.println("Creation Date: " + info.getCreationDate());
            System.out.println("Modification Date: " + info.getModificationDate());
            System.out.println("Keywords: " + info.getKeywords());

            // You can also access custom metadata
            for (String key : info.getMetadataKeys()) {
                System.out.println(key + ": " + info.getCustomMetadataValue(key));
            }

            document.close();
        } catch (Exception e) {
            System.err.println("Error reading metadata for " + file.getPath() + ": " + e.getMessage());
        }
    }

    public static String convert (String input){
        return input.replace(".", "").replace(',', '.');
    }

    public static String findLineStartingWith(String[] lines, String prefix) {
        for (String line : lines) {
            if (line.startsWith(prefix)) {
                return line;  // return the matching line
            }
        }
        return null;  // return null if no match is found
    }

    public static synchronized void traceFolder(File directory,HelloController hc){
        TracingFolderEvent tfe = new TracingFolderEvent(hc);
        hc.fireStartTracingFolder(tfe);
        Serialization.createFileIfNotExists(directory.getPath());

        hc.listManager.getFailed().clear();
        hc.listManager.getChecksums().clear();
        hc.listManager.fetchChecksums();
        filetracing(directory,hc);

        System.out.println("the files in folder "+hc.listManager.getFilesInFolderQueue().size());
        hc.numFilesInFolder.set(hc.listManager.getFilesInFolderQueue().size());


        int i=0;
        while (!hc.listManager.getFilesInFolderQueue().isEmpty()){
            i++;
            File tempFile = hc.listManager.getFilesInFolderQueue().poll();
            TextExtractions.checkFile(tempFile,hc.listManager);
            hc.percentOfTracing.set((double) i / hc.numFilesInFolder.get());
        }
        hc.fireEndTracingFolder(tfe);
    }


    public static void filetracing(File directory, HelloController hc){
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    filetracing(file, hc);
                } else {
                    hc.listManager.addToFilesInFolder(file);
                }
            }
        }
    }

    public static void checkFile(File file, ListManager listManager){
        if(!file.getName().toLowerCase().endsWith(".pdf")){
            Document notPdf = new Document(file.getPath());
            notPdf.getErrorList().add("not pdf");
            listManager.addToFailed(notPdf); // add to failed list - how will we manage it
            return;
        }

        String checksum = calculatePDFChecksum(file);

        if(listManager.getChecksums().contains(checksum)){
            //edo eixame diaxeirisi etsi oste
            //na menei to enimeronomai to path
            //me to pio prosfato checksum
            Document doc = new Document(file.getPath());
            doc.addToErrorList("duplicate");
            listManager.addToFailed(doc);
            return;
        }
        for (Document d :listManager.getToImportQueue().toList()){
            assert checksum != null;
            if (d.getChecksum().compareTo(checksum)==0){
                Document doc = new Document(file.getPath());
                doc.addToErrorList("duplicate");
                listManager.addToFailed(doc);
                System.out.println("the checksum is at the toImportQueue");
                return;
            }
        }

        Document doc = new Document(file.getPath());
        doc.setChecksum(checksum);
        listManager.addToImportQueue(doc);
    }

    public static void process(Document document, HelloController controller){
        //we check and handle the case that the documented that has been sent
        //for process is a duplicate
        if (controller.listManager.getChecksums().contains(document.getChecksum())){
            /*
            Document toUpdDoc = Document.getDocumentByChecksum(controller.listManager,document.getChecksum());
            if(toUpdDoc!=null){
                toUpdDoc.updateDocumentsFile(document.getFilePath(),controller.listManager);
            } else {
                System.out.println("the checksum contains but cannot retrieve a document");
            }
            System.out.println("this file is duplicate:---> "+document.getFilePath());
             */
            System.out.println("we found a duplicate file - need to added the code for how to handle it - - - - - - - - - - ");
            return;
        }
        controller.listManager.getChecksums().add(document.getChecksum());


        //we attempt to extract the contents of the document
        //if we fail the document goes to the failed list
        //so we can check later what went wrong
        if (!extractContents(document)){
            controller.listManager.addToFailed(document);
            document.getErrorList().add("error at extracting line");
            System.out.println("couldn't extract contents / parts of the document file");
            return;
        }

        //here we check what type of pdf document we are trying to process
        //if it is the usual type of AB Invoice we process it accordingly
        //
        //at the end, if no proper way to extract the document is found
        //then we add the document to the failed list
        if (ABUsualInvoice.isValidABInvoice(document.getParts().get(0))){
            ABUsualInvoice.process(document, controller);
            return;
        }
        if(ABServiceInvoice.isValid(document.getParts().get(0))){
            ABServiceInvoice.process(document, controller);
            controller.listManager.addToImported(document);
            return;
        }
        if(ABNonPriced.isValid(document.getParts().get(0))){
            ABNonPriced.process(document);
            controller.listManager.addToImported(document);
            return;
        }
        if(ABCancellingInvoice.isValid(document.getParts().get(0))){
            ABCancellingInvoice.process(document,controller);
            controller.listManager.addToImported(document);
            return;
        }
        if(ABSupportingCancellation.isValid(document.getParts().get(0))){
            ABSupportingCancellation.process(document);
            controller.listManager.addToImported(document);
            return;
        }
        if(ABSurplusTicket.isValid(document.getParts().get(0))){
            ABSurplusTicket.process(document);
            controller.listManager.addToImported(document);
            return;
        }

        //System.out.println("- - - - - - - - - - - - - - - - - - - ");
        //System.out.println(parts[0]);
        //System.out.println("- - - - - - - - - - - - - - - - - - - ");

        //if the process reached here it means that although we extracted the
        //contents we didn't have to get the data, so we also add the document
        //to the failed list so we can find out what went wrong
        document.getErrorList().add("no valid extractors");
        controller.listManager.addToFailed(document);
        System.out.println("couldn't find the correct extractor for the document "+document.getFilePath());
    }

    public static boolean extractContents(Document document){
        try {
            PdfReader reader = new PdfReader(new File(document.getFilePath()).getAbsolutePath());
            String[] parts = new String[reader.getNumberOfPages()];
            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                parts[i-1] = PdfTextExtractor.getTextFromPage(reader,i);
            }
            document.setParts(Arrays.stream(parts).toList());
            reader.close();
            return true;
        } catch (IOException e) {
            System.err.println("Error extracting text using iText: " + e.getMessage());
            return false;
        }
    }
}

// to file tracing -> koitaei ta arxeia pou vriskontai sto fakelo kai vazei ta pdf sti lista traced
// sta nea arxeia ipologizetai automata to checksum
