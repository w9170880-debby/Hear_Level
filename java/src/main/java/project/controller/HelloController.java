package project.controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

    @RestController
    @CrossOrigin(origins = "http://localhost:5173") // מאפשר ל-React להתחבר
    public class HelloController {

        @GetMapping("/api/hello")
        public String sayHello() {
            return " /הודעה מהשרת: החיבור ל-Java הצליח!";
        }

}

