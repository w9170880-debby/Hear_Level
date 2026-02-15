package project.dto;

import project.model_entity.UserRole;

// זהו אובייקט פשוט שמייצג את הנתונים שנשלח מה-React
public class UserRequest {
    private String email;
    private String password;
    private String name;
    private UserRole role;

    // הגטרים והסתרים מאפשרים ל-Spring Boot "למלא" את האובייקט בנתונים
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
}