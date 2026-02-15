package project.model_entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;
import java.util.ArrayList;
@Document(collection = "users") // זה שם האוסף במונגו
public class User {
    @Id
    private String id; // מונגו אוהב String ID
    private String name;
    private String email;
    private String password;
    private UserRole role;

    // רשימה של מזהי עבודות
    private List<String> workIds = new ArrayList<>();
    public User() {}


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getWorkIds() {
        return workIds;
    }

    public void setWorkIds(List<String> workIds) {
        this.workIds = workIds;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
}
