package io.undertree.demo.storedproc;

import io.undertree.demo.storedproc.domain.DummyEntity;
import io.undertree.demo.storedproc.domain.SampleDataRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.EntityManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class DemoBootStoredProcApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoBootStoredProcApplication.class, args);
    }

    /**
     * Simple 2 REST endpoints to invoke a function and stored procedure
     */
    @RestController
    static class DefaultController {
        private final SampleDataRepository statesRepository;
        private final JdbcTemplate jdbcTemplate;
        private final EntityManager entityManager;

        DefaultController(SampleDataRepository statesRepository, JdbcTemplate jdbcTemplate, EntityManager entityManager) {
            this.statesRepository = statesRepository;
            this.jdbcTemplate = jdbcTemplate;
            this.entityManager = entityManager;
        }

        @GetMapping("/err08003")
        void getError08003() {
            statesRepository.error08003();
        }

        @GetMapping("/err08006")
        void getError08006() {
            statesRepository.error08006();
        }

        @GetMapping("/add-func")
        Integer getResult(@RequestParam(value = "arg1", defaultValue = "0") int arg1, @RequestParam(value = "arg2", defaultValue = "0") int arg2) {
            return statesRepository.addFunc(arg1, arg2);
        }

        @GetMapping("/increment-proc")
        Integer increment(@RequestParam(value = "arg", defaultValue = "0") int arg) {
            return statesRepository.incrementProc(arg);
        }

        @GetMapping("/fetch-dummy")
        List<DummyEntity> fetchDummy() {
            var list = new ArrayList<DummyEntity>();

            try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
                connection.setAutoCommit(false); // this is important!!!

                try (CallableStatement callableStatement = connection.prepareCall("{call fetch_dummy(?)}")) {
                    callableStatement.setNull(1, Types.OTHER);
                    callableStatement.registerOutParameter(1, Types.REF_CURSOR);
                    callableStatement.execute();

                    try (ResultSet results = (ResultSet) callableStatement.getObject(1)) {
                        while (results.next()) {
                            list.add(new DummyEntity(results.getInt(1), results.getString(2)));
                        }
                    }
                }

                connection.commit();
                connection.setAutoCommit(true);
            } catch (SQLException sqlException) {
                sqlException.printStackTrace();
            }

            return list;
        }
    }
}
