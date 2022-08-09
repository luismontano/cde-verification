package cdeverification.cdeverification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CDEVeritifactionService implements CDEVerification{

    public static final String CANTIDADES = "__ CANTIDADES";
    public static final String MODELOS = "__ MODELOS";
    public static final String PLANOS = "__ PLANOS";
    private Logger logger = LoggerFactory.getLogger(CDEVeritifactionService.class);
    private final String HID = "FCI-JAMC-HID";
    private final String GEO = "FCI-JAMC-GEO";
    private final String EST = "FCI-JAMC-EST";
    private final String BIM = "FCI-JAMC-BIM";
    private final String CON = "FCI-JAMC-CON";
    private final Set<String> AREAS = new LinkedHashSet<>(Arrays.asList(HID, GEO, EST, BIM, CON));
    private final String WIP = "WIP";
    private final String SHD = "SHD";
    private final String PUB = "PUB";

    @Value("${cde.path}")
    private String CDEPath;

    @Value("${cde.path.WIP}")
    private String WIPPath;

    @Value("${cde.path.SHD}")
    private String SHDPath;

    @Value("${cde.path.PUB}")
    private String PUBPath;

    @Override
    public void verifyCDE() throws IOException {

        Map<String, String> pathByFolder = new LinkedHashMap<String, String>() {{
            put(WIP, WIPPath);
            put(SHD, SHDPath);
            put(PUB, PUBPath);
        }};

        logger.info("1. Información CDE:");
        logger.info("\tRuta CDE: {}", CDEPath);
        pathByFolder.forEach((folder, path) -> logger.info("\tRuta {}: {}", folder, WIPPath));

        logger.info("2. Verificando si existen archivos con errores:");
        pathByFolder.forEach((folder, path) -> {
            try {
                logBadPaths(folder, path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        Map<String, Set<String>> WIPPathsByAreaPath = getCorrectPaths(WIPPath);
        Map<String, Set<String>> SHDPathsByAreaPath = getCorrectPaths(SHDPath);
        Map<String, Set<String>> PUBPathsByAreaPath = getCorrectPaths(PUBPath);
        Map<String, Integer> totalsByArea = new LinkedHashMap<>();

        WIPPathsByAreaPath.entrySet().stream().forEach(e -> {
            int total = totalsByArea.get(e.getKey()) != null ? totalsByArea.get(e.getKey()) : 0;
            totalsByArea.put(e.getKey(), total + e.getValue().size());
        });

        SHDPathsByAreaPath.entrySet().stream().forEach(e -> {
            int total = totalsByArea.get(e.getKey()) != null ? totalsByArea.get(e.getKey()) : 0;
            totalsByArea.put(e.getKey(), total + e.getValue().size());
        });

        PUBPathsByAreaPath.entrySet().stream().forEach(e -> {
            int total = totalsByArea.get(e.getKey()) != null ? totalsByArea.get(e.getKey()) : 0;
            totalsByArea.put(e.getKey(), total + e.getValue().size());
        });

        int total = totalsByArea.values().stream().reduce(0, Integer::sum);
        logger.info("3. Verificando trazabilidad de archivos de [{}] desde [{}]:", PUB, SHD);
        logPaths(PUB, PUBPath, PUBPathsByAreaPath, SHDPathsByAreaPath);

        logger.info("4. Verificando trazabilidad de archivos de [{}] desde [{}]:", SHD, WIP);
        logPaths(SHD, SHDPath, SHDPathsByAreaPath, WIPPathsByAreaPath);

        Set<String> PUBPathsForModels = getPathsForObjects(PUBPath, MODELOS);
        Set<String> SHDPathsForModels = getPathsForObjects(SHDPath, MODELOS);
        logger.info("5. Verificando trazabilidad de modelos de [{}] desde [{}]:", PUB, SHD);
        logPaths(PUBPathsForModels, SHDPathsForModels);

        Set<String> PUBPathsForPlan = getPathsForObjects(PUBPath, PLANOS);
        Set<String> SHDPathsForPlan = getPathsForObjects(SHDPath, PLANOS);
        logger.info("6. Verificando trazabilidad de planos de [{}] desde [{}]:", PUB, SHD);
        logPaths(PUBPathsForPlan, SHDPathsForPlan);

        Set<String> PUBPathsForTables = getPathsForObjects(PUBPath, CANTIDADES);
        Set<String> SHDPathsForTables = getPathsForObjects(SHDPath, CANTIDADES);
        logger.info("7. Verificando trazabilidad de planos de [{}] desde [{}]:", PUB, SHD);
        logPaths(PUBPathsForTables, SHDPathsForTables);

        logger.info("Total de Archivos Correctos: {}", total);
        totalsByArea.entrySet().stream().forEach(e -> logger.info("\tTotal de Archivos Correctos {}: {}", e.getKey(), e.getValue()));
    }

    private Map<String, Set<String>> getCorrectPaths(String folderPath) throws IOException {
        return Files
                .find(Paths.get(folderPath), Integer.MAX_VALUE, (filePath, fileAttr) -> fileAttr.isRegularFile())
                .filter(path -> path.getFileName().toString().matches("FCI-JAMC-HID.*-V\\d\\d\\d\\..*")
                        || path.getFileName().toString().matches("FCI-JAMC-GEO.*-V\\d\\d\\d\\..*")
                        || path.getFileName().toString().matches("FCI-JAMC-BIM.*-V\\d\\d\\d\\..*")
                        || path.getFileName().toString().matches("FCI-JAMC-CON.*-V\\d\\d\\d\\..*")
                        || path.getFileName().toString().matches("FCI-JAMC-EST.*-V\\d\\d\\d\\..*"))
                .map(path -> path.getFileName().toString())
                .collect(Collectors.groupingBy(
                        name -> name.substring(0, 12), HashMap::new, Collectors.toCollection(HashSet::new)
                ));
    }

    private Set<String> getPathsForObjects(String folderPath, String object) throws IOException {
        return Files
                .find(Paths.get(folderPath), Integer.MAX_VALUE, (filePath, fileAttr) -> fileAttr.isRegularFile())
                .filter(path -> path.getFileName().toString().matches("FCI-JAMC-HID.*\\..*")
                        || path.getFileName().toString().matches("FCI-JAMC-GEO.*\\..*")
                        || path.getFileName().toString().matches("FCI-JAMC-BIM.*\\..*")
                        || path.getFileName().toString().matches("FCI-JAMC-CON.*\\..*")
                        || path.getFileName().toString().matches("FCI-JAMC-EST.*\\..*"))
                .map(path -> path.getFileName().toString())
                .filter(path -> path.toString().contains(object))
                .collect(Collectors.toSet());
    }

    private Map<String, Set<String>> getBadPaths(String folderPath) throws IOException {

        Set<String> filesToIgnore = new HashSet<String>(Arrays.asList(
                "FCI-JAMC-BIM-Logo.png",
                "FCI-JAMC-BIM-SeguimientoYControlGraficas.xlsx",
                "FCI-JAMC-BIM-SeguimientoYControlGraficas.pbix",
                "FCI-JAMC-BIM-ParametrosCompartidos.txt"
        ));

        return Files
                .find(Paths.get(folderPath), Integer.MAX_VALUE, (filePath, fileAttr) -> fileAttr.isRegularFile())
                .filter(path -> path.getFileName().toString().matches("FCI-JAMC-.*"))
                .filter(path -> !path.getFileName().toString().matches("FCI-JAMC-.*-V\\d\\d\\d\\..*"))
                // Files to Ignore
                .filter(path -> !filesToIgnore.contains(path.getFileName().toString()))
                // Extensions to Ignore
                .filter(path -> !path.getFileName().toString().endsWith(".rfa"))
                // Folders to Ignore
                .filter(path -> !path.toString().contains(CANTIDADES)
                        && !path.toString().contains(MODELOS)
                        && !path.toString().contains(PLANOS))
                .map(path -> path.getFileName().toString())
                .collect(Collectors.groupingBy(
                        name -> name.substring(0, 12), HashMap::new, Collectors.toCollection(HashSet::new)
                ));
    }

    private void logPaths(String folder, String path, Map<String, Set<String>> pathsByAreaTo, Map<String, Set<String>> pathsByAreaFrom) throws IOException {

        Map<String, Set<String>> badPathsByArea = new LinkedHashMap<>();
        Integer pathsCount = pathsByAreaTo.values().stream().map(s -> s != null ? s.size() : 0).reduce(0, Integer::sum);
        logger.info("\t[{}] Total archivos analizados: {}", folder, pathsCount);
        AREAS.forEach(area -> {
            Set<String> pathsFrom = pathsByAreaFrom.get(area) == null ? new LinkedHashSet<>() : pathsByAreaFrom.get(area);
            Set<String> pathsTo = pathsByAreaTo.get(area) == null ? new LinkedHashSet<>() : pathsByAreaTo.get(area);
            badPathsByArea.put(area, pathsTo.stream().filter(file -> !pathsFrom.contains(file))
                    .collect(Collectors.toSet()));
        });
        logBadPaths(badPathsByArea);
    }

    private void logBadPaths(String folder, String path) throws IOException {
        Map<String, Set<String>> badPathsByArea = getBadPaths(path);
        Integer badPathsCount = badPathsByArea.values().stream().map(s -> s != null ? s.size() : 0).reduce(0, Integer::sum);
        logger.info("\t[{}] Total archivos con errores: {}", folder, badPathsCount);
        logBadPaths(badPathsByArea);
    }
    private void logBadPaths(Map<String, Set<String>> badPathsByArea) {
        AREAS.forEach(area -> {
            Set<String> badPaths = badPathsByArea.get(area) == null ? new HashSet<String>() : badPathsByArea.get(area);
            logger.info("\t\tArchivos con Errores para {}: {}", area, badPaths.size());
            badPaths.forEach(bp -> logger.info("\t\t\t{}", bp));
        });
    }

    private void logPaths(Set<String> PUBPathsForObject, Set<String> SHDPathsForObject) {
        Set<String> badPaths = PUBPathsForObject.stream().filter(file -> !SHDPathsForObject.contains(file))
                .collect(Collectors.toSet());
        logger.info("\tTotal archivos con errores: {}", badPaths.size());
        badPaths.forEach(bp -> logger.info("\t\t\t{}", bp));
    }
}
