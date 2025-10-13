package amios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class MainApplication {
    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }
}

// Create a separate controller for REST endpoints
@RestController
class CustomerController {



    @GetMapping("/customer")
    public customer getCustomer() {
        return new customer(1L, "James Bond");
    }
}

// Use proper class naming convention
//class Customer {
//    private final Long id;
//    private final String name;
//
//    public Customer(Long id, String name) {
//        this.id = id;
//        this.name = name;
//    }
//
//    public Long getId() {
//        return id;
//    }
//
//    public String getName() {
//        return name;
//    }
//}
