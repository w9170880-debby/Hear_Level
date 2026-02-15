package project.controller;

import project.dto.UserRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.model_entity.User;
import project.repository.UserRepository;

@RestController
@RequestMapping("/api/auth") // כל הכתובות של הדף הזה יתחילו ב-api/auth
@CrossOrigin(origins = "http://localhost:5173") // מאפשר ל-React (פורט 5173) לדבר עם Java
public class AuthController {
    private final UserRepository userRepository;
    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // פונקציית הרשמה - מקבלת נתונים ושומרת אותם (כרגע רק מדפיסה)
    @PostMapping("signup")
    public ResponseEntity<?> signup(@RequestBody UserRequest request) {
        // בדיקה האם המשתמש קיים במערכת
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("האימייל כבר קיים במערכת");
        }
        //יצירת אובייקט משתמש חדש
        User newUser = new User();
        newUser.setEmail(request.getEmail());
        newUser.setPassword(request.getPassword()); //כדאי להצפין את הסיסמא בהמשך
        newUser.setName(request.getName());
        newUser.setRole(request.getRole());
        // שמירה במונגו
        User savedUser = userRepository.save(newUser);

        System.out.println("נרשם משתמש חדש: " + request.getEmail());
        return ResponseEntity.ok(savedUser);    }

    // פונקציית התחברות - בודקת אם האימייל והסיסמה נכונים
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserRequest request) {
        return userRepository.findByEmail(request.getEmail()).
                filter(user -> user.getPassword().equals(request.getPassword())).
                map(user -> {
                    System.out.println( user.getEmail() + "התחברות הצליחה");
                    return ResponseEntity.ok(user);}).
                orElseGet(() -> {
                    System.out.println("שגיאה בהתחברות");
                    return ResponseEntity.status(401).build();});
    }
}