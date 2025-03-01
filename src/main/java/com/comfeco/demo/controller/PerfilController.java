package com.comfeco.demo.controller;

import com.comfeco.demo.entity.Perfil;
import com.comfeco.demo.entity.Usuario;
import com.comfeco.demo.service.IPerfilService;
import com.comfeco.demo.service.IUsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RequestMapping("/api/perfil")
@RestController
public class PerfilController {

    @Autowired
    private IPerfilService perfilService;

    @Autowired
    private IUsuarioService usuarioService;

    @PutMapping(produces = "application/json", consumes = "application/json")
    private ResponseEntity<?> editar(@RequestBody Perfil perfil){
        Map<String, Object> rs = new HashMap<>();
        Usuario u = this.usuarioService.findByCorreo(SecurityContextHolder.getContext().getAuthentication().getName());

        if(u.getUsuId() != null) {
            this.perfilService.modificar(perfil);
            rs.put("message", "Operación realizada correctamente");
            rs.put("code", 200);
            return new ResponseEntity<>(rs, HttpStatus.OK);
        } else {
            rs.put("message", "Usuario no encontrado y/o no autenticado");
            rs.put("code", 500);
            return new ResponseEntity<>(rs, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping
    private ResponseEntity<?> listar(){
        return new ResponseEntity<>(this.perfilService.listar(), HttpStatus.OK);
    }

    @PutMapping(value="/editar/avatar", produces = MediaType.APPLICATION_JSON_VALUE)
    private ResponseEntity<?> editarAvatar(@RequestBody String url){
        Map<String, Object> rs = new HashMap<>();
        Perfil p = this.perfilService.cambiarAvatar(url);
        if(p.getIdPerfil() != null){
            rs.put("message", "Operación realizada correctamente");
            rs.put("code", 200);
            return  new ResponseEntity<>(rs, HttpStatus.OK);
        } else {
            rs.put("message", "Perfil no encontrado y/o no autenticado");
            rs.put("code", 500);
            return new ResponseEntity<>(rs, HttpStatus.BAD_REQUEST);
        }
    }
}
