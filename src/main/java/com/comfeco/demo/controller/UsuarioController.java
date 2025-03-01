package com.comfeco.demo.controller;

import com.comfeco.demo.entity.ConfirmationToken;
import com.comfeco.demo.entity.Perfil;
import com.comfeco.demo.entity.Usuario;
import com.comfeco.demo.exception.ModeloNotFoundException;
import com.comfeco.demo.service.IConfirmationTokenService;
import com.comfeco.demo.service.IEmailService;
import com.comfeco.demo.service.IUsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Security;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/usuario")
public class UsuarioController {

    @Autowired
    private IUsuarioService usuarioService;

    @Autowired
    private IConfirmationTokenService confirmationTokenService;

    @Autowired
    private IEmailService emailService;

    @Autowired
    private BCryptPasswordEncoder bcrypt;

    @PostMapping(produces = "application/json", consumes = "application/json")
    private ResponseEntity<?> registrar(@RequestBody Usuario usuario){
        Map<String, Object> response = new HashMap<>();

        Perfil perfil = new Perfil();

        usuario.setUsuClave(bcrypt.encode(usuario.getUsuClave()));
        usuario.setPerfil(perfil);

        try {
            usuarioService.registrarTransaccional(usuario);
            response.put("message", "Registrado correctamente");
            response.put("code", 200);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        catch (Exception ex){
            response.put("message", ex.getMessage());
            response.put("code", 500);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping(consumes = "application/json", produces = "application/json")
    private ResponseEntity<?> editar(@RequestBody Usuario usuario){
        Map<String, Object> rs = new HashMap<>();

        Usuario u = this.usuarioService.findByCorreo(SecurityContextHolder.getContext().getAuthentication().getName());

        if(u.getUsuId() != null){
            if(usuario.getUsuClave() == null){
                usuario.setUsuClave(u.getUsuClave());
                usuario.setRoles((u.getRoles()));
                this.usuarioService.modificar(usuario);
                rs.put("message", "Usuario modificado correctamente");
                rs.put("status", 200);
            } else {
                String claveNueva = usuario.getUsuClave();
                boolean coinciden = bcrypt.matches(claveNueva, u.getUsuClave());
                if(!coinciden){
                    usuario.setUsuClave(bcrypt.encode(claveNueva));
                    usuario.setRoles((u.getRoles()));
                    rs.put("message", "Datos y contraseña cambiados correctamente");
                    rs.put("status", 200);
                }
                else
                    usuario.setUsuClave(u.getUsuClave());
                    usuario.setRoles((u.getRoles()));
                    this.usuarioService.modificar(usuario);
                    rs.put("message", "Usuario modificado correctamente");
                    rs.put("status", 200);
            }
           return new ResponseEntity<>(rs, HttpStatus.OK);
        } else {
            throw new ModeloNotFoundException("Usuario no autenticado");
        }
    }


    @GetMapping("/page/{page}")
    private Page<Usuario> listar(@PathVariable Integer page){
        Pageable pageable = PageRequest.of(page, 10);
        return usuarioService.listar(pageable);
    }

    @GetMapping
    private ResponseEntity<Usuario> listarUsuarioAutenticado(){
        Usuario u = this.usuarioService.findByCorreo(SecurityContextHolder.getContext().getAuthentication().getName());
        if(u.getUsuId() != null){
            return new ResponseEntity<>(u, HttpStatus.OK);
        } else {
            throw new ModeloNotFoundException("Usuario no encontrado");
        }
    }


    @GetMapping("/buscar/nickname/{nickname}")
    private ResponseEntity<Usuario> buscarByNickname(@PathVariable String nickname){
        return new ResponseEntity<>(usuarioService.findByNickname(nickname), HttpStatus.OK);
    }

    @GetMapping("/buscar/correo/{correo}")
    private ResponseEntity<Usuario> buscarByCorreo(@PathVariable String correo){
        return new ResponseEntity<>(usuarioService.findByCorreo(correo), HttpStatus.OK);
    }

    @GetMapping("/recuperar/{email}")
    private ResponseEntity<?> recuperarCuenta(@PathVariable String email){
        Map<String, Object> rs = new HashMap<>();
        Usuario u = this.usuarioService.findByCorreo(email);
        if(u.getUsuId() != null) {
            ConfirmationToken ct = new ConfirmationToken(u);
            this.confirmationTokenService.save(ct);

            // CREO EL CORREO
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(u.getUsuCorreo());
            mailMessage.setSubject("Recupera tu cuenta");
            mailMessage.setFrom("gusramirez0101@gmail.com");
            mailMessage.setText("Para cambiar tu contraseña click al siguiente link: " + "http://localhost:4200/auth/restore/" + ct.getConfirmationToken());
            this.emailService.sendEmail(mailMessage);

            rs.put("message", "Correo electrónico enviado");
            rs.put("code", 200);

            return new ResponseEntity<>(rs, HttpStatus.OK);
        } else {
            rs.put("message", "Token no encontrado y/o expirado");
            rs.put("code", 500);

            return new ResponseEntity<>(rs, HttpStatus.BAD_REQUEST);
        }
    }

}
