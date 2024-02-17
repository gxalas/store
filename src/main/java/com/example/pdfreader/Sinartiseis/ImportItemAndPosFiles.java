package com.example.pdfreader.Sinartiseis;

import com.example.pdfreader.DAOs.*;
import com.example.pdfreader.DTOs.ProductWithAttributes;
import com.example.pdfreader.Entities.Attributes.StoreBasedAttributes;
import com.example.pdfreader.Entities.Main.Product;
import com.example.pdfreader.Entities.Main.EntriesFile;
import com.example.pdfreader.HelloController;
import com.example.pdfreader.Entities.ChildEntities.PosEntry;
import com.example.pdfreader.enums.StoreNames;
import com.example.pdfreader.enums.SySettings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
/*
right now what we do with the store based attributes
if they are not 930 we use the mastercode to find the product
if it is a 930 we use the barcodes to infer the product
 */

public class ImportItemAndPosFiles {
    private final Map<File, StoreNames> itemFiles = new HashMap<>();
    private final Map<File, StoreNames> posSaleFiles = new HashMap<>();
    private final Map<String, ProductWithAttributes> barcodeToProductWithAttributes = new HashMap<>();
    private final Map<String, Product> masterToProduct = new HashMap<>();
    private final Map<StoreNames,Map<String, StoreBasedAttributes>> sbaMegaMap= new HashMap<>();
    private final Map<StoreNames,Map<Date,Map<String, PosEntry>>> megaPosMap = new HashMap<>();
    private final List<String> hardConflicts = new ArrayList<>(); // we found more than one product for a sba
    private  final List<String> softConflicts = new ArrayList<>(); // the product found has not a department as the sba
    private final HelloController hc;
    private final Lock lock = new ReentrantLock();

    public ImportItemAndPosFiles(HelloController hc) {
        this.hc = hc;
        // Initialization of other necessary components.
        // No need to lock in the constructor since object construction is not a concurrent operation.
    }
    public void initiateLoadingProcess() {
        lock.lock();
        try {
            loadItemAndTextFiles();
        } finally {
            lock.unlock();
        }
    }


    private void loadItemAndTextFiles() {
        // Method's heavy lifting is now wrapped within a lock in the public method `initiateLoadingProcess`
        //hc.listManager.loadFileChecksums();
        getFilesFromFolder();
        readFiles();
        attempt();

        System.out.println("\n\n\n we F i n i s h e d ! ! !");

        //initBarcodeToProductMap(); //from the database
        //initMasterToProductMap(); //from the database -- should be the same method as the above

        //Map<StoreNames,Map<String,StoreBasedAttributes>> sbasToSave = filterSbasNeeded();
        //Set<Product> newProductsCreatedBySbas = createNewProducts(sbasToSave);



        //Map<StoreNames,Map<String,Product>> existingProducts = getToSaveSbas(sbasToSave);



        //saveStoreBasedAttributes(sbasToSave.stream().toList());


        //savePosEntries();


        //clearMaps();
    }
    private void clearMaps() {
        itemFiles.clear();
        posSaleFiles.clear();
        barcodeToProductWithAttributes.clear();
        masterToProduct.clear();
        megaPosMap.clear();
        sbaMegaMap.clear();
    }



    private  void getFilesFromFolder(){
        File parentFolder = SySettings.txtFolderPath.toFile();
        if (parentFolder.exists() && parentFolder.isDirectory()) {
            File[] filesAndFolders = parentFolder.listFiles();
            if (filesAndFolders != null) {
                for (File file : filesAndFolders) {
                    if (file.isDirectory()) {
                        for (StoreNames storeName : StoreNames.values()) {
                            if (file.getName().compareTo(storeName.getDescription()) == 0) {
                                List<File> txtFiles = EntriesFile.getTxtFilesInFolder(file.getPath());
                                for (File f : txtFiles) {
                                    if (f.getName().toLowerCase().contains("items")) {
                                        itemFiles.put(f, storeName);
                                    } else if (f.getName().toLowerCase().contains("possales")) {
                                        posSaleFiles.put(f, storeName);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private  void readFiles(){
        itemFiles.forEach(this::createTheMegaSbaMap);
        posSaleFiles.forEach(this::createTheMegaPosMap);
    }



    private void assignProductsFromTheSbas(List<StoreBasedAttributes> sbas){
        List<Product> newProducts = new ArrayList<>();
        sbas.forEach(sba->findProductWithBarcodes(sba,newProducts));


        //findProductWithBarcodes();

        Map<StoreNames,Map<String,Product>> dbProductMap = new HashMap<>();
        Map<StoreNames,Map<String,Product>> filesProductMap = new HashMap<>();

        filesProductMap.keySet().forEach(store->{
            filesProductMap.get(store).keySet().forEach(master->{
                if(dbProductMap.get(store).get(master)!=null){

                }
            });
        });

    }
    private  void findProductWithBarcodes(StoreBasedAttributes sba, List<Product> toSaveProducts) {
        Set<ProductWithAttributes> matchingProducts = new HashSet<>();
        sba.getBarcodes().forEach(barcode->{
            if(barcodeToProductWithAttributes.get(barcode)!=null){
                matchingProducts.add(barcodeToProductWithAttributes.get(barcode));
            }
        });
        /*
        - - null master->
        we cannot do anything, it must be loaded a new items file,
        what is the cost of this error??
        we are having a posEntry without having a product
        we can create a product from a custom Pos Entry : product.attributes.put(customStore,sba)

        - - softConflict ->
        we have to "accept" the possible conflict

        - - hardConflicting ->
        we have to remove the conflicting barcodes from the wrong item

        ##
        so, we have to flag the kind of error that its sba is facing

        * in case of a soft conflict we can add the matching products in an attribute
        of the sba (eg. possible products)

        * in case of hard conflict we have to be able to view :
        the SBA without a product assigned
        the possible products that are more than 1 (in case of hard conflicts)
        */

        if(matchingProducts.isEmpty()){
            if(sba.getProduct()==null){
                Product product = new Product();
                product.setLog("created by sba - "+sba.getStore()+"\n at empty matching");
                product.setInvDescription(sba.getDescription());
                product.setInvmaster(sba.getMasterCode());
                if(masterToProduct.get(sba.getMasterCode())==null){
                    masterToProduct.put(sba.getMasterCode(),product);
                } else {
                    System.out.println(" mysterious thing ");
                }
                sba.setProduct(product);
                ProductWithAttributes tempPWA = new ProductWithAttributes(product,sba);
                sba.getBarcodes().forEach(barcode->{
                    barcodeToProductWithAttributes.put(barcode,tempPWA);
                });
                toSaveProducts.add(product);
            }
        } else if(matchingProducts.size()==1){
            Set<String> matchingDeparments = new HashSet<>();
            matchingProducts.stream().toList().get(0).getAttributes().values().forEach(mSba->{
                matchingDeparments.add(mSba.getDepartment());
            });
            if (matchingDeparments.contains(sba.getDepartment())){
                Product p = matchingProducts.stream().toList().get(0).getProduct();
                if(sba.getProduct()==null){
                    if(!p.getLog().contains("matching product 1")){
                        p.setLog(p.getLog()+"\n at matching product 1");
                    }
                    sba.setProduct(p);
                } else {
                    if(sba.getProduct().equals(p)){
                        //ok
                    } else {
                        //conflict
                        sba.getProduct().setLog(sba.getProduct().getLog()+"\n c o n f l i c t");
                        p.setLog(p.getLog()+"\nconflict");
                        sba.setProduct(p);
                        //sba.setProduct(null);
                    }
                }
            } else {
                String softLog = "probable conflict "+sba.getStore()+", "+sba.getDepartment()+" :: "+sba.getDescription()+", matching depts  "+matchingDeparments+" = "+matchingProducts.stream().toList().get(0).getProduct().getInvDescription();
                if(!softConflicts.contains(softLog)){
                    softConflicts.add(softLog);
                }
                if(false){
                    // these conflict have to be
                    // the matching products do not have the same department with the sba
                    // we have to add manually the product to the sba, and then probably the departments will much
                    System.out.println("probable conflict "+sba.getStore()+", "+sba.getDepartment()+" :: "+sba.getDescription()+", matching depts  "+matchingDeparments+" = "+matchingProducts.stream().toList().get(0).getProduct().getInvDescription());
                }
            }
        } else {
            //here are the conflicts that we have more than one matching products
            //this is caused because one store has two products properly
            //and then the other store has only one of these products
            //and for some reason that product has both of the barcodes
            // - - - - - - - - - Handling
            //in the product, we go to the attribute "conflicting barcodes", and we add to the
            //list the barcode of the wrong product - that will be ignored - and put it to the
            //map of the store: product.get(store).put(CONFLICTING_BARCODE);
            String hardLog = "hard conflict : "+sba.getMasterCode()+" , "+sba.getStore()+" , "+sba.getDescription();
            if(!hardConflicts.contains(hardLog)){
                hardConflicts.add(hardLog);
            }

        }
    }
    private  void createTheMegaPosMap(File file, StoreNames storeName){
        System.out.println("- processing : "+file.getName()+", for store : "+storeName.getName());
        String fChecksum = TextExtractions.calculateChecksum(file);
        if(hc.listManager.getFileChecksums().contains(fChecksum)){
            System.out.println("The file: " + file.getName() + " already exists "+fChecksum);
            return;
        }

        megaPosMap.computeIfAbsent(storeName, k -> new HashMap<>());
        Date indexDate = null;
        int index =0;
        try{
            Path path = file.toPath();
            List<String> possales = Files.readAllLines(path, Charset.forName("ISO-8859-7"));
            try {
                for (String possale : possales) {
                    if (possale.trim().compareTo("") != 0) {
                        PosEntry temp = new PosEntry(possale, storeName);
                        if(indexDate==null){
                            indexDate=temp.date;
                        }
                        if(indexDate.compareTo(temp.date)!=0){
                            index = 0;
                            indexDate = temp.date;
                        }
                        temp.setShaCode(temp.sha256(index));
                        index++;
                        megaPosMap.get(storeName).computeIfAbsent(temp.getDate(), k -> new HashMap<>());
                        megaPosMap.get(storeName).get(temp.getDate()).putIfAbsent(temp.getMaster(), temp);
                    }
                }
            } catch (Exception e){
                System.out.println("\n\n\n\n\n");
                System.out.println("an error happened while extraction possales");
                System.out.println("\n\n\n\n\n");
                e.printStackTrace();
                System.out.println("\n\n\n\n\n");
                System.err.println(e.getMessage());
            }
            System.out.println("read complete - - - - - ");
        } catch (IOException e){
            System.err.println("Error reading possales \n"+e.getMessage());
            e.printStackTrace();
        }
        EntriesFileDAO entriesFileDAO = new EntriesFileDAO(HibernateUtil.getSessionFactory());
        entriesFileDAO.saveEntriesFile(new EntriesFile(file.getPath(), fChecksum));
    }
    private  void createTheMegaSbaMap(File file, StoreNames store){
        String newline = "";
        Map<String,Product> barcodeToProduct = new HashMap<>();
        sbaMegaMap.computeIfAbsent(store, k -> new HashMap<>());
        try (BufferedReader br = new BufferedReader(new FileReader(file, Charset.forName("ISO-8859-7")))) {
            while (((newline = br.readLine()) != null) && (newline.length()>7)) { //to length megalitero tou 7 thelei allo elengxo

                StoreBasedAttributes currentSba = sbaFromLine(newline);
                currentSba.setStore(store);
                if(sbaMegaMap.get(store).get(currentSba.getMasterCode())==null){
                    //if current sba is not at the map
                    sbaMegaMap.get(store).put(currentSba.getMasterCode(),currentSba);
                } else {
                    //current sba is on the map -> update
                    updateSba(sbaMegaMap.get(store).get(currentSba.getMasterCode()),currentSba);
                }

                if(currentSba.getHope().compareTo("3050325")==0){
                    StoreBasedAttributes sba = sbaMegaMap.get(currentSba.getStore()).get(currentSba.getMasterCode());
                    System.out.println(" :: "+newline+" :: ");
                    System.out.println("we found "+sba.getDescription()+" "+sba.getBarcodes()+" "+sba.getHopeBarcodes());
                }
                //here we check if there is a conflict
                //with the barcodes
                //if(barcodeToProduct)
            }
            System.out.println("READJUSTING STARTS");
            sbaMegaMap.keySet().forEach(key->{
                sbaMegaMap.get(key).keySet().forEach(master->{
                    StoreBasedAttributes sba = sbaMegaMap.get(key).get(master);
                    if(sba.getHope().compareTo("3050325")==0){
                        System.out.println("we found it at readjusting ");
                    }
                    if(sba.getBarcodes().isEmpty()){
                        if(sba.getHope().compareTo("3050325")==0){
                            System.out.println("bars are empty");
                        }
                        sba.getBarcodes().addAll(sba.getHopeBarcodes());
                    }
                });
            });
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (Exception e){
            System.out.println("error at trying to read lines at "+file.getName());
            e.printStackTrace();
        }
    }
    private  StoreBasedAttributes sbaFromLine(String line){
        StoreBasedAttributes sba = new StoreBasedAttributes();
        String master = line.substring(0, 11).trim();
        String hope = line.substring(11,18).trim();
        String description = line.substring(18, 53).trim();
        String dept = line.substring(92,95).trim();
        String barcode = line.substring(79,92).trim();
        sba.setDescription(description);
        sba.setMasterCode(master);
        sba.setHope(hope);
        sba.setDepartment(dept);



        if(sba.getHope().length()>6){

            if(sba.getHope().compareTo("3050325")==0){
                System.out.println("length > 6");
            }

            if((barcode.startsWith(sba.getHope().substring(0,6)))
                    && (sba.getFamily().compareTo("930")==0)
                    && (barcode.length()<9)){

                if(sba.getHope().compareTo("3050325")==0){
                    System.out.println("complex true");
                }
                sba.getHopeBarcodes().add(barcode);
            } else {

                if(sba.getHope().compareTo("3050325")==0){
                    System.out.println("complex not true");
                }


                if((barcode.contains(sba.getHope().substring(sba.getHope().length()-5)))&&barcode.length()<9){

                    if(sba.getHope().compareTo("3050325")==0){
                        System.out.println("contains true");
                    }

                    sba.getHopeBarcodes().add(barcode);
                } else {

                    if(sba.getHope().compareTo("3050325")==0){
                        System.out.println("contains not true");
                    }

                    sba.getBarcodes().add(barcode);
                }
            }
        } else {

            if(sba.getHope().compareTo("3050325")==0){
                System.out.println("lenght <=  6");
            }
        }

        return sba;
    }
    private  void initBarcodeToProductMap(){
        barcodeToProductWithAttributes.clear();
        List<StoreBasedAttributes> sbas = new ArrayList<>();
        StoreBasedAttributesDAO storeBasedAttributesDAO = new StoreBasedAttributesDAO();
        sbas.addAll(storeBasedAttributesDAO.getAllStoreBasedAttributes());
        sbas.forEach(sba->{
            sba.getBarcodes().forEach(bar->{
                if(barcodeToProductWithAttributes.get(bar)!=null){
                    if(!barcodeToProductWithAttributes.get(bar).getProduct().equals(sba.getProduct())){
                        System.out.println(" C O N F L I C T ");
                    }
                } else {
                    if(sba.getProduct()!=null){
                        ProductWithAttributes tempPWA = new ProductWithAttributes(sba.getProduct(),sba);
                        sba.getBarcodes().forEach(barcode->{
                            if(barcodeToProductWithAttributes.get(barcode)==null){
                                barcodeToProductWithAttributes.put(barcode,tempPWA);
                            } else {
                                sba.getConflictingBarcodes().add(barcode);
                            }
                        });
                    }
                }
            });
        });
    }
    private  void initMasterToProductMap(){
        masterToProduct.clear();
        ProductDAO productDAO = new ProductDAO();
        StoreBasedAttributesDAO storeBasedAttributesDAO = new StoreBasedAttributesDAO();
        List<Product> products = new ArrayList<>(productDAO.getAllProducts());
        List<StoreBasedAttributes> sbas = new ArrayList<>(storeBasedAttributesDAO.getAllStoreBasedAttributes());

        sbas.forEach(sba-> {
            if(sba.getFamily().compareTo("930")!=0){
                if(sba.getProduct()!=null){
                    masterToProduct.computeIfAbsent(sba.getMasterCode(), k -> sba.getProduct());
                }
            }
        });
        sbas.clear();
        products.forEach(product -> {
            if(product.getInvmaster()!=null){
                masterToProduct.putIfAbsent(product.getInvmaster(), product);
            }
        });
        products.clear();
    }


    private  void attempt(){
        StoreBasedAttributesDAO storeBasedAttributesDAO = new StoreBasedAttributesDAO();
        List<StoreBasedAttributes> dbSbas = storeBasedAttributesDAO.getAllStoreBasedAttributes();

        Map<String,Product> barcodeToProduct = new HashMap<>();
        Map<StoreNames,Map<String,StoreBasedAttributes>> dbSbasMap = new HashMap<>();

        Map<StoreNames,Map<String,Product>> masterToProductMap = new HashMap<>();


        dbSbas.forEach(sba->{
            sba.getBarcodes().forEach(barcode->{
                if(barcodeToProduct.get(barcode)!=null){
                    System.out.println("that is weird !!!#");
                } else {
                    if(sba.getProduct()!=null){
                        barcodeToProduct.put(barcode,sba.getProduct());
                    }
                }
            });
            dbSbasMap.computeIfAbsent(sba.getStore(), k -> new HashMap<>());
            dbSbasMap.get(sba.getStore()).putIfAbsent(sba.getMasterCode(), sba);

            masterToProductMap.computeIfAbsent(sba.getStore(), k -> new HashMap<>());
            if(masterToProductMap.get(sba.getStore()).get(sba.getMasterCode())==null){
                if(sba.getProduct()!=null){
                    masterToProductMap.get(sba.getStore()).put(sba.getMasterCode(),sba.getProduct());
                }
            }
        });
        dbSbas.clear();
        dbSbas = null;

        PosEntryDAO posEntryDAO = new PosEntryDAO();
        List<PosEntry> allPoses = posEntryDAO.getAllPosEntries();
        Map<StoreNames,Map<Date,Map<String,PosEntry>>> megaDbPosMap = new HashMap<>();

        allPoses.forEach(pos->{
            megaDbPosMap.computeIfAbsent(pos.getStoreName(), k -> new HashMap<>());
            megaDbPosMap.get(pos.getStoreName()).computeIfAbsent(pos.getDate(), k -> new HashMap<>());
            megaDbPosMap.get(pos.getStoreName()).get(pos.getDate()).putIfAbsent(pos.getMaster(), pos);
        });

        allPoses.clear();
        allPoses= null;

        List<PosEntry> toSavePosEntries = new ArrayList<>();

        megaPosMap.keySet().forEach(store->{
            megaPosMap.get(store).keySet().forEach(date->{
                megaPosMap.get(store).get(date).keySet().forEach(master->{
                    //3050325
                    StoreBasedAttributes s = sbaMegaMap.get(store).get(master);
                    megaPosMap.get(store).get(date).get(master).setSba(sbaMegaMap.get(store).get(master));
                    if(megaDbPosMap.get(store)==null){
                        toSavePosEntries.add(megaPosMap.get(store).get(date).get(master));
                    } else {
                        if(megaDbPosMap.get(store).get(date)==null){
                            toSavePosEntries.add(megaPosMap.get(store).get(date).get(master));
                        } else {
                            if(megaDbPosMap.get(store).get(date).get(master)==null){
                                toSavePosEntries.add(megaPosMap.get(store).get(date).get(master));
                            }
                        }
                    }
                });
            });
        });

        megaPosMap.clear();
        Map<StoreBasedAttributes,List<PosEntry>> sbaToPoses = new HashMap<>();

        toSavePosEntries.forEach(posEntry -> {
            if(posEntry.getSba()==null){
                System.out.println("we found a pos without an sba");
            } else{
                sbaToPoses.computeIfAbsent(posEntry.getSba(), k -> new ArrayList<>());
                sbaToPoses.get(posEntry.getSba()).add(posEntry);
            }

        });
        Set<StoreBasedAttributes> toUpdateSbas = new HashSet<>();
        Set<StoreBasedAttributes> toSaveSbas = new HashSet<>();
        Set<Product> toSaveProducts = new HashSet<>();

        sbaToPoses.keySet().forEach(sba->{
            if(dbSbasMap.get(sba.getStore())!=null){
                if(dbSbasMap.get(sba.getStore()).get(sba.getMasterCode())!=null){
                    StoreBasedAttributes dbSba = dbSbasMap.get(sba.getStore()).get(sba.getMasterCode());
                    checkSba(dbSba,sba);
                    sba.getBarcodes().forEach(barcode->{
                        if((!dbSba.getBarcodes().contains(barcode)||dbSba.getBarcodes()==null)
                                &&(!dbSba.getConflictingBarcodes().contains(barcode)||dbSba.getConflictingBarcodes()==null)
                                &&(!dbSba.getHopeBarcodes().contains(barcode))||dbSba.getHopeBarcodes()==null){
                            if(barcodeToProduct.get(barcode)!=null){
                                if(barcodeToProduct.get(barcode).equals(sba.getProduct())){
                                    dbSba.getBarcodes().add(barcode);
                                }
                            } else {
                                dbSba.getBarcodes().add(barcode);
                            }
                        }
                    });
                    sbaToPoses.get(sba).forEach(posEntry -> {
                        posEntry.setSba(dbSba);
                    });
                    toUpdateSbas.add(dbSba);
                } else{
                    findProductOfSba(sba,barcodeToProduct,masterToProductMap);
                    toSaveSbas.add(sba);
                    toSaveProducts.add(sba.getProduct());
                }
            } else {
                findProductOfSba(sba,barcodeToProduct,masterToProductMap);
                toSaveSbas.add(sba);
                toSaveProducts.add(sba.getProduct());
            }
        });

        ProductDAO productDAO = new ProductDAO();
        productDAO.saveProducts(toSaveProducts.stream().toList());

        storeBasedAttributesDAO = new StoreBasedAttributesDAO();
        storeBasedAttributesDAO.updateStoreBasedAttributes(toUpdateSbas.stream().toList());

        storeBasedAttributesDAO = new StoreBasedAttributesDAO();
        storeBasedAttributesDAO.saveSBAs(toSaveSbas.stream().toList());

        posEntryDAO = new PosEntryDAO();
        posEntryDAO.savePosEntries(toSavePosEntries);

        System.out.println("the to save products are "+toSaveProducts.size());
        System.out.println("the to save pos are "+toSavePosEntries.size());
        System.out.println("the to update sbas are "+toUpdateSbas.size());

    }
    private Set<Product> createNewProducts (Map<StoreNames,Map<String,StoreBasedAttributes>> sbasMap){

        Set<Product> newProducts = new HashSet<>();
        Map<StoreNames,Map<String,Product>> productMap = new HashMap<>();
        Map<String,Product> barMap = new HashMap<>();

        AtomicInteger empties = new AtomicInteger(0);
        AtomicInteger one = new AtomicInteger(0);
        AtomicInteger other = new AtomicInteger(0);

        for (StoreNames store : sbasMap.keySet()) {
            productMap.put(store, new HashMap<>());
            sbasMap.get(store).keySet().forEach(master -> {

                StoreBasedAttributes sba = sbasMap.get(store).get(master);

                Set<Product> matchingProducts = new HashSet<>();
                sba.getBarcodes().forEach(barcode -> {
                    if (barMap.get(barcode) != null) {
                        matchingProducts.add(barMap.get(barcode));
                    }
                });
                if (matchingProducts.isEmpty()) {
                    empties.getAndIncrement();
                    //asxeto check
                    if (sba.getMasterCode().compareTo(master) != 0) {
                        System.err.println("Periergo check");
                    }
                    if (productMap.get(store).get(master) == null) {
                        Product product;
                        product = new Product();
                        product.setLog("\n created by file");
                        product.setInvmaster(sba.getMasterCode());
                        product.setInvDescription(sba.getDescription());
                        productMap.get(store).put(master, product);
                        sba.setProduct(product);
                        sba.getBarcodes().forEach(barcode -> {
                            barMap.put(barcode, product);
                        });
                    } else {
                        if (sba.getProduct() != null) {
                            System.err.println("periergo 2");
                        }
                        sba.setProduct(productMap.get(store).get(master));
                        sba.getBarcodes().forEach(barcode -> {
                            barMap.put(barcode, sba.getProduct());
                        });
                    }

                } else if (matchingProducts.size() == 1) {
                    one.getAndIncrement();
                    sba.setProduct(matchingProducts.stream().toList().get(0));
                    sba.getBarcodes().forEach(barcode -> {
                        if (barMap.get(barcode) == null) {
                            barMap.put(barcode, sba.getProduct());
                        }
                    });
                } else {
                    other.getAndIncrement();
                    if (other.get() < 10) {
                        matchingProducts.forEach(product -> {
                            System.out.println("a conflicting product " + product.getInvDescription());
                        });
                    }
                    //conflict
                }
                if (sba.getProduct() != null) {
                    newProducts.add(sba.getProduct());
                }
            });
        }
        System.out.println("the no result are "+empties.get()+", the ones are "+one.get()+", the more are "+other.get());
        return newProducts;
    }

    private void checkSba(StoreBasedAttributes dbSba, StoreBasedAttributes fileSba){
        dbSba.setHope(fileSba.getHope());
        dbSba.setFamily(fileSba.getFamily());
        dbSba.setDepartment(fileSba.getDepartment());
        dbSba.setDescription(fileSba.getDescription());
    }
    private void findProductOfSba(StoreBasedAttributes sba,
                                   Map<String,Product> barcodeToProduct,
                                   Map<StoreNames,Map<String,Product>> masterToProductMap){

        Set<Product> matchingProducts = new HashSet<>();
        sba.getBarcodes().forEach(barcode -> {
            if (barcodeToProduct.get(barcode) != null) {
                matchingProducts.add(barcodeToProduct.get(barcode));
            }
        });
        if(sba.getProduct()!=null){
            System.err.println("we are trying to find a product for an sba that already has an product");
        }
        if (matchingProducts.isEmpty()) {
            //empties.getAndIncrement();
            //asxeto check

            Product product = new Product();
            boolean found = false ;
            if(masterToProductMap.get(sba.getStore())!=null){
                if(masterToProductMap.get(sba.getStore()).get(sba.getMasterCode())!=null) {
                    product = masterToProductMap.get(sba.getStore()).get(sba.getMasterCode());
                    found = true;
                    sba.setProduct(product);
                    // here is the thing with the barcodes
                    sba.getBarcodes().addAll(sbaMegaMap.get(sba.getStore()).get(sba.getMasterCode()).getBarcodes());
                }
            }

            if(!found){
                sba.setProduct(createNewProduct(sba));
                sba.getBarcodes().forEach(barcode->{
                    barcodeToProduct.put(barcode,sba.getProduct());
                });
                if(masterToProductMap.get(sba.getStore())==null){
                    masterToProductMap.put(sba.getStore(),new HashMap<>());
                    masterToProductMap.get(sba.getStore()).put(sba.getMasterCode(),sba.getProduct());
                } else {
                    masterToProductMap.get(sba.getStore()).put(sba.getMasterCode(),sba.getProduct());
                }
            }


        } else if (matchingProducts.size() == 1) {
            //one.getAndIncrement();
            sba.setProduct(matchingProducts.stream().toList().get(0));
            sba.getBarcodes().forEach(barcode -> {
                barcodeToProduct.computeIfAbsent(barcode, k -> sba.getProduct());
            });
            masterToProductMap.computeIfAbsent(sba.getStore(), k -> new HashMap<>());
            masterToProductMap.get(sba.getStore()).put(sba.getMasterCode(),sba.getProduct());
        } else {
            //other.getAndIncrement();
            //if (other.get() < 10) {
            //    matchingProducts.forEach(product -> {
            //        System.out.println("a conflicting product " + product.getInvDescription());
            //    });
            //}
            //conflict
        }
    }

    private Product createNewProduct(StoreBasedAttributes sba){
        Product product = new Product();
        product.setInvmaster(sba.getMasterCode());
        product.setInvDescription(sba.getDescription());
        product.setLog("\n created by file");
        return product;
    }






    private  Map<StoreNames,Map<String,StoreBasedAttributes>> filterSbasNeeded(){
        List<Product> newProducts = new ArrayList<>();
        Map <StoreNames,Map<String,StoreBasedAttributes>> sbasToSave = new HashMap<>();

        //gia kathe pos entry pame na vroume to antisoixo sba apo to items file
        //den einai kako auto
        //List<String> nullMasters = new ArrayList<>(); // no sba found with the pos master -> probably item file older than pos file

        megaPosMap.keySet().forEach(store->{
            megaPosMap.get(store).keySet().forEach(date->{
                for (String master : megaPosMap.get(store).get(date).keySet()) {
                    StoreBasedAttributes sba = sbaMegaMap.get(store).get(master);
                    if (sba == null) {
                        // here i think we cant find a store based attribute for this product on the store
                        // probably because the item file is older that the pos file
                        String k = master+" | | "+store+" | | "+megaPosMap.get(store).get(date).get(master).getDescription()+" | | ";
                        //if(!nullMasters.contains(k)){
                        //    nullMasters.add(k);
                        //}
                        continue;
                    }
                    /*
                    //from the database
                    findProductWithMasterCode(sba);
                    //we didn't find a product from the database so
                    //we try to find a product through the barcode form
                    //the database
                    // in the find product with barcodes, if
                    // we don't find a matching product for the barcodes
                    // of the sba we create a new product and assign it also
                    // to the master to product
                    findProductWithBarcodes(sba, newProducts);

                    if (sba.getProduct() == null) {
                        System.out.println("sba without product "+sba.getDescription());
                        //System.out.println("couldn't find a product");
                    }
                    if (sba.getProduct() == null) {

                    }
                     */
                    megaPosMap.get(store).get(date).get(master).setSba(sba);

                    sbasToSave.computeIfAbsent(sba.getStore(), k -> new HashMap<>());
                    if(sbasToSave.get(sba.getStore()).get(sba.getMasterCode())!=null){
                        if(sbasToSave.get(sba.getStore()).get(sba.getMasterCode()).equals(sba)){
                            //everything ok, do nothing
                        } else {
                            System.out.println("error, different sbas for same mastercode and same store");
                        }
                    } else {
                        sbasToSave.get(sba.getStore()).put(sba.getMasterCode(),sba);
                    }
                    //sbasToSave.add(sba);
                }
            });
        });
        //System.out.println("\n the null masters are "+nullMasters.size());
        return sbasToSave;
    }
    private  void saveStoreBasedAttributes(List<StoreBasedAttributes> toSave){
        StoreBasedAttributesDAO storeBasedAttributesDAO = new StoreBasedAttributesDAO();

        List<StoreBasedAttributes> dbSbas = new ArrayList<>();
        dbSbas.addAll(storeBasedAttributesDAO.getAllStoreBasedAttributes());

        Map<StoreNames,Map<String,StoreBasedAttributes>> dbSbaMap = new HashMap<>();
        Map<StoreNames,Map<String,Product>> dbProductMap = new HashMap<>();
        dbSbas.forEach(sba->{
            dbSbaMap.computeIfAbsent(sba.getStore(), k -> new HashMap<>());
            dbProductMap.computeIfAbsent(sba.getStore(), k -> new HashMap<>());
            dbSbaMap.get(sba.getStore()).put(sba.getMasterCode(),sba);
            dbProductMap.get(sba.getStore()).put(sba.getMasterCode(),sba.getProduct());
        });
        dbSbas.clear();
        dbSbas = null;

        List<StoreBasedAttributes> saveList =  new ArrayList<>();
        toSave.forEach(sba->{
            if(dbSbaMap.get(sba.getStore())!=null){
                if(dbSbaMap.get(sba.getStore()).get(sba.getMasterCode())!=null){
                    if(dbSbaMap.get(sba.getStore()).get(sba.getMasterCode()).areEqual(sba)){
                        //do not save
                    } else {
                        System.out.println("the sba has changed - should be updated");
                        //there is an error
                    }
                } else {
                    saveList.add(sba);
                    //save
                }
            } else {
                saveList.add(sba);
            }
        });

        ProductDAO productDAO = new ProductDAO();
        List<Product> products = productDAO.getAllProducts();

        /*
        Map<String,Product> mapProducts = new HashMap<>();

        products.forEach(product->{
            if(mapProducts.get(product.getInvmaster())!=null){
                if(product.getInvmaster()!=null)
                    System.out.println("not expected while creating product map");
            } else {
                mapProducts.put(product.getInvmaster(),product);
            }
        });
         */

        AtomicInteger counter = new AtomicInteger();



        Set<Product> toSaveProducts = new HashSet<>();
        Set<Product> toUpdProducts = new HashSet<>();

        saveList.forEach(sba->{
            if(dbProductMap.get(sba.getStore())!=null){
                if(dbProductMap.get(sba.getStore()).get(sba.getMasterCode())!=null){
                    sba.setProduct(dbProductMap.get(sba.getStore()).get(sba.getMasterCode()));
                }
            }

            Map<String,Product> mapProducts = new HashMap<>();

            if(sba.getFamily().compareTo("930")!=0){
                if(mapProducts.get(sba.getMasterCode())!=null){
                    Product p = mapProducts.get(sba.getMasterCode());
                    p.setLog(p.getLog()+"\nat saving");
                    sba.setProduct(p);
                    counter.getAndIncrement();
                    toUpdProducts.add(sba.getProduct());
                } else{
                    toSaveProducts.add(sba.getProduct());
                }
            }else{
                toSaveProducts.add(sba.getProduct());
            }
        });

        System.out.println("the moved products are "+counter+" from the possible "+saveList.size());
        System.out.println("the to saved set is "+toSaveProducts.size());
        System.out.println("the to update set is "+toUpdProducts.size());


        productDAO = new ProductDAO();

        if(!toSaveProducts.isEmpty()){
            try {
                productDAO.saveProducts(toSaveProducts.stream().toList());
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        if(saveList.isEmpty()){
            return;
        }
        storeBasedAttributesDAO.saveSBAs(saveList);
    }

    private  void savePosEntries(){
        if(megaPosMap.isEmpty()){
            return;
        }
        PosEntryDAO posEntryDAO = new PosEntryDAO();
        List<PosEntry> allPosEntries = posEntryDAO.getAllPosEntries();

        System.out.println("the pos entries at the database are "+allPosEntries.size());

        Map<StoreNames,Map<Date,Map<String,PosEntry>>> dbPosMap = new HashMap<>();
        allPosEntries.forEach(posEntry -> {
            dbPosMap.computeIfAbsent(posEntry.getStoreName(), k -> new HashMap<>());
            dbPosMap.get(posEntry.storeName).computeIfAbsent(posEntry.getDate(),k->new HashMap<>());
            if(dbPosMap.get(posEntry.storeName).get(posEntry.getDate()).get(posEntry.getMaster())!=null){
                System.err.println("\n\n a pos entry is double on the map \n\n");
            }
            dbPosMap.get(posEntry.storeName).get(posEntry.getDate()).putIfAbsent(posEntry.getMaster(), posEntry);
        });

        List<PosEntry> toSave = new ArrayList<>();
        megaPosMap.keySet().forEach(store->{
            megaPosMap.get(store).keySet().forEach(date->{
                megaPosMap.get(store).get(date).values().forEach(pos->{
                    if(dbPosMap.get(store)==null){
                        toSave.add(pos);
                    } else if (dbPosMap.get(store).get(date)==null){
                        toSave.add(pos);
                    } else if(dbPosMap.get(store).get(date).get(pos.getMaster())==null){
                        //should save
                        if(!toSave.contains(pos)){
                            toSave.add(pos);
                        } else {
                            System.err.println("the pos already exists in the to save list");
                        }

                    } else {
                        //should check if they are the same
                    }
                });
            });
        });
        System.out.println("we are attempting to save the pos entries "+toSave.size());

        posEntryDAO = new PosEntryDAO();
        posEntryDAO.savePosEntries(toSave);
    }
    private  void updateSba(StoreBasedAttributes oldSba,StoreBasedAttributes newSba){
        if(oldSba.getDepartment().compareTo(newSba.getDepartment())!=0){
            oldSba.setDepartment(newSba.getDepartment());
            System.out.println("the department got updated for "+oldSba.getMasterCode());
        }
        if(oldSba.getHope().compareTo(newSba.getHope())!=0){
            oldSba.setHope(newSba.getHope());
            System.out.println("the hope changed for "+oldSba.getMasterCode());
        }

        if(oldSba.getDescription().compareTo(newSba.getDescription())!=0){
            oldSba.setDescription(newSba.getDescription());
            System.out.println("the department changed for "+oldSba.getMasterCode());
        }

        newSba.getBarcodes().forEach(bar->{
            if(!oldSba.getBarcodes().contains(bar)){
                if(!oldSba.getConflictingBarcodes().contains(bar)){
                    oldSba.getBarcodes().add(bar);
                    //System.out.println("a new barcode added for "+oldSba.getMasterCode());
                }
            }
        });
        newSba.getConflictingBarcodes().forEach(bar->{
            if(!oldSba.getConflictingBarcodes().contains(bar)){
                oldSba.getConflictingBarcodes().add(bar);
            }
        });
        newSba.getHopeBarcodes().forEach(bar->{
            if(!oldSba.getHopeBarcodes().contains(bar)){
                if(!oldSba.getConflictingBarcodes().contains(bar)){
                    oldSba.getHopeBarcodes().add(bar);
                    //System.out.println("a new barcode added for "+oldSba.getMasterCode());
                }
            }
        });
    }

    private  void findProductWithMasterCode(StoreBasedAttributes sba){
        /*
        * we wil try to remove this check
        * and add afterward a general check
        * that will find the conflicts and clear them
        *
        * second check. get all barcodes and if we find
        * multiple matching products for product will get
        * the sbas that reference those products and remove
        * those products
        *
        * later we will go to the conflicts tab and
        * resolve them
        */

        if(sba.getDepartment().compareTo("31")!=0){

        }
        if(sba.getProduct()!=null){
            System.out.println(" we shouldn't have a product for an sba yet");
        }

        if(masterToProduct.get(sba.getMasterCode())!=null){
            Product p = masterToProduct.get(sba.getMasterCode());
            if(!p.getLog().contains("by mastercode")){
                p.setLog(p.getLog()+"\n find by mastercode");
            }
            sba.setProduct(p);
        }

        /*

         */
        /*

        if(sba.getProduct()!=null){
            Product temp = sba.getProduct();
            AtomicBoolean conflict = new AtomicBoolean(false);
            sba.getBarcodes().forEach(bar-> {
                if (barcodeToProductWithAttributes.get(bar)!=null){
                    if(barcodeToProductWithAttributes.get(bar).getProduct()!=null){
                        if(!barcodeToProductWithAttributes.get(bar).getProduct().equals(temp)){
                            conflict.set(true);
                        }
                        */

                        /*
                        if(!barcodeToProductWithAttributes.get(bar).getProduct().equals(sba.getProduct())){
                            System.out.println(" c o n f l i c t "+sba.getDescription()+" "+
                                    barcodeToProductWithAttributes.get(bar).getProduct().getInvDescription());
                            sba.setProduct(null);
                        }
                         */
                        /*
                    }
                }
            });
            if(conflict.get()){
                sba.setProduct(null);
            }



        }

                         */
/*
DElete
private  Map<StoreNames,Map<String,StoreBasedAttributes>> newAttempt(){
        Set<PosEntry> toSavePos = new HashSet<>();
        Set<PosEntry> toUpdatePos = new HashSet<>();

        PosEntryDAO posEntryDAO = new PosEntryDAO();
        List<PosEntry> dbPosEntries = posEntryDAO.getAllPosEntries();
        Map<StoreNames,Map<Date,Map<String,PosEntry>>> dbMapPosEntries = new HashMap<>();
        dbPosEntries.forEach(posEntry -> {
            dbMapPosEntries.computeIfAbsent(posEntry.storeName, k -> new HashMap<>());
            dbMapPosEntries.get(posEntry.storeName).computeIfAbsent(posEntry.getDate(), k -> new HashMap<>());
            dbMapPosEntries.get(posEntry.storeName).get(posEntry.getDate()).putIfAbsent(posEntry.getMaster(), posEntry);
            // check if pos has sba, check if the poses are the same
    });

        dbPosEntries.clear();
    dbPosEntries = null;

    StoreBasedAttributesDAO storeBasedAttributesDAO = new StoreBasedAttributesDAO();
    List<StoreBasedAttributes> dbSbas = storeBasedAttributesDAO.getAllStoreBasedAttributes();
    Map<StoreNames,Map<String,StoreBasedAttributes>> dbMapSba = new HashMap<>();

        dbSbas.forEach(sba->{
        dbMapSba.computeIfAbsent(sba.getStore(), k -> new HashMap<>());
        dbMapSba.get(sba.getStore()).putIfAbsent(sba.getMasterCode(), sba);
    });
        dbSbas.clear();
    dbSbas = null;

        megaPosMap.keySet().forEach(store->{
        megaPosMap.get(store).keySet().forEach(date->{
            megaPosMap.get(store).get(date).keySet().forEach(master -> {

                StoreBasedAttributes sba = sbaMegaMap.get(store).get(master);

                if(dbMapSba.get(store)!=null){
                    if(dbMapSba.get(store).get(master)!=null){
                        sba = dbMapSba.get(store).get(master);
                    }
                }

                megaPosMap.get(store).get(date).get(master).setSba(sba);
                if(dbMapPosEntries.get(store)==null){
                    toSavePos.add(megaPosMap.get(store).get(date).get(master));
                } else if (dbMapPosEntries.get(store).get(date)==null){
                    toSavePos.add(megaPosMap.get(store).get(date).get(master));
                } else if(dbMapPosEntries.get(store).get(date).get(master)==null){
                    toSavePos.add(megaPosMap.get(store).get(date).get(master));
                } else {
                    //here we need to check and update - by shouldn't be necessary
                }
            });
        });
    });
        megaPosMap.clear();

    Map<StoreNames,Map<String,StoreBasedAttributes>> toSaveSbasMap = new HashMap<>();
    //Set<StoreBasedAttributes> toSaveSbas = new HashSet<>();
    Set<StoreBasedAttributes> toUpdateSba = new HashSet<>();



        toSavePos.forEach(posEntry -> {
        if(posEntry.getSba()==null){
            //this shouldn't happen
            //check if we added an pos without sba//
        } else {
            toSaveSbasMap.computeIfAbsent(posEntry.storeName, k -> new HashMap<>());
            toSaveSbasMap.get(posEntry.storeName).computeIfAbsent(posEntry.getMaster(), k -> posEntry.getSba());
        }
    });

        toSaveSbasMap.keySet().forEach(store->{
        toSaveSbasMap.get(store).keySet().forEach(master->{
            if(dbMapSba.get(store)!=null){
                if(dbMapSba.get(store).get(master)!=null){
                    toSaveSbasMap.get(store).get(master) = dbMapSba.get(store).get(master);


                    dbMapSba.get(store).get(master) =
                         = dbMapSba.get(store).get(master);
                    toUpdateSba.add(sba);
                    toSaveSbasMap.remove(sba);
                }
            }
        });
    });




    //megaPosMap = null;


        return null;
}


 */


    }
}
