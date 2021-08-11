package io.undertree.demo.storedproc;

import io.undertree.demo.storedproc.domain.SampleDataRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

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

        DefaultController(SampleDataRepository statesRepository) {
            this.statesRepository = statesRepository;
        }

        @GetMapping("/add-func")
        Integer getResult(@RequestParam(value = "arg1", defaultValue = "0") int arg1, @RequestParam(value = "arg2", defaultValue = "0") int arg2) {
            return statesRepository.addFunc(arg1, arg2);
        }

        @GetMapping("/increment-proc")
        Integer increment(@RequestParam(value = "arg", defaultValue = "0") int arg) {
            return statesRepository.incrementProc(arg);
        }
    }
}
