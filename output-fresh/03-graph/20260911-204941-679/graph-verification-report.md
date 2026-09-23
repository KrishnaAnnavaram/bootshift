# Application Graph Verification Report

**Result:** PASSED

| Check | Value |
|---|---|
| java_files_inventoried | 63 |
| java_files_in_graph | 63 |
| java_file_coverage | 1.0 |
| modules_in_build_model | 6 |
| modules_in_graph | 6 |
| types_modelled | 63 |
| type_nodes_in_graph | 63 |
| controllers | 5 |
| endpoints | 10 |
| spring_components | 25 |
| distinct_libraries_in_build_model | 300 |
| library_nodes_in_graph | 300 |
| harness_nodes_in_graph | 0 |
| attribution_ratio | 0.6858 |
| unresolved_or_ambiguous_nodes | 44 |
| graph_query_integrity | OK |
| graph_query_detail | blast radius from TYPE:com.aura.vihanga.employeeservice.repository.EmployeeRepository returned 7 node(s); transitive dependencies returned 25 |

## Representative edge spot checks

| Edge | Evidence |
|---|---|
| MODULE:configuaration-server-[DEPENDS_ON_LIBRARY]->LIB:org.springframework.boot:spring-boot-starter-actuator | com.aura.vihanga:configuaration-server |
| MODULE:configuaration-server-[DEPENDS_ON_LIBRARY]->LIB:org.springframework.boot:spring-boot-starter | com.aura.vihanga:configuaration-server |
| MODULE:configuaration-server-[DEPENDS_ON_LIBRARY]->LIB:org.springframework.boot:spring-boot | com.aura.vihanga:configuaration-server |
| MODULE:configuaration-server-[DEPENDS_ON_LIBRARY]->LIB:org.springframework:spring-context | com.aura.vihanga:configuaration-server |
| MODULE:configuaration-server-[DEPENDS_ON_LIBRARY]->LIB:org.springframework.boot:spring-boot-autoconfigure | com.aura.vihanga:configuaration-server |
| MODULE:configuaration-server-[DEPENDS_ON_LIBRARY]->LIB:org.springframework.boot:spring-boot-starter-logging | com.aura.vihanga:configuaration-server |
| MODULE:configuaration-server-[DEPENDS_ON_LIBRARY]->LIB:ch.qos.logback:logback-classic | com.aura.vihanga:configuaration-server |
| MODULE:configuaration-server-[DEPENDS_ON_LIBRARY]->LIB:ch.qos.logback:logback-core | com.aura.vihanga:configuaration-server |
| MODULE:configuaration-server-[DEPENDS_ON_LIBRARY]->LIB:org.apache.logging.log4j:log4j-to-slf4j | com.aura.vihanga:configuaration-server |
| MODULE:configuaration-server-[DEPENDS_ON_LIBRARY]->LIB:org.apache.logging.log4j:log4j-api | com.aura.vihanga:configuaration-server |
| MODULE:configuaration-server-[DEPENDS_ON_LIBRARY]->LIB:org.slf4j:jul-to-slf4j | com.aura.vihanga:configuaration-server |
| MODULE:configuaration-server-[DEPENDS_ON_LIBRARY]->LIB:jakarta.annotation:jakarta.annotation-api | com.aura.vihanga:configuaration-server |
