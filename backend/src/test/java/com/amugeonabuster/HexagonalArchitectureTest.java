package com.amugeonabuster;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.onionArchitecture;

@AnalyzeClasses(packages = "com.amugeonabuster", importOptions = ImportOption.DoNotIncludeTests.class)
public class HexagonalArchitectureTest {

    @ArchTest
    public static final ArchRule 헥사고날_의존성_무결성_검증 = onionArchitecture()
            .domainModels("..domain..")
            .domainServices("..application.service..")
            .applicationServices("..application..")
            .adapter("web", "..adapter.in.web..")
            .adapter("websocket-in", "..adapter.in.websocket..")
            .adapter("websocket-out", "..adapter.out.websocket..")
            .adapter("persistence", "..adapter.out.persistence..")
            .adapter("external", "..adapter.out.external..")
            .adapter("mock", "..adapter.out.mock..")
            .adapter("scheduler", "..adapter.in.scheduler..");
}
