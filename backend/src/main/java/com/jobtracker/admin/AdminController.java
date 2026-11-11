package com.jobtracker.admin;

import com.jobtracker.admin.AdminService.Statistiques;
import com.jobtracker.admin.AdminService.Utilisateur;
import com.jobtracker.common.CurrentUser;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** API d'administration : chaque appel verifie d'abord que l'utilisateur connecte est administrateur. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/statistiques")
    public Statistiques statistiques() {
        adminService.verifierAdmin(CurrentUser.id());
        return adminService.statistiques();
    }

    @GetMapping("/utilisateurs")
    public List<Utilisateur> utilisateurs() {
        adminService.verifierAdmin(CurrentUser.id());
        return adminService.utilisateurs();
    }

    @PostMapping("/utilisateurs/{id}/deconnexion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deconnecter(@PathVariable Long id) {
        adminService.verifierAdmin(CurrentUser.id());
        adminService.deconnecter(id);
    }

    public record RoleRequest(boolean admin) {
    }

    @PutMapping("/utilisateurs/{id}/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changerRole(@PathVariable Long id, @RequestBody RoleRequest req) {
        Long adminId = CurrentUser.id();
        adminService.verifierAdmin(adminId);
        adminService.changerRole(adminId, id, req.admin());
    }

    @DeleteMapping("/utilisateurs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        adminService.verifierAdmin(CurrentUser.id());
        adminService.supprimer(id);
    }
}
