package com.reactive.vos.webflux.app.controllers;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.thymeleaf.spring5.context.webflux.ReactiveDataDriverContextVariable;

import com.reactive.vos.webflux.app.documents.Producto;
import com.reactive.vos.webflux.app.services.ProductoService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SessionAttributes("producto")
@Controller
public class ProductoController {
	
	private static final Logger log = LoggerFactory.getLogger(ProductoController.class);
	
	@Autowired
	private ProductoService service;
	
	
	@GetMapping({"/listar", "/"})
	public Mono<String> listar(Model model) {
		
		Flux<Producto> productos = service.findAllWithNameUpperCase();
		
		productos.subscribe(prod -> log.info(prod.getNombre()));
		
		model.addAttribute("productos", productos);
		model.addAttribute("titulo", "Listado de Productos");
		log.info("productos" + productos);
		return Mono.just("listar");
	}
	
	@GetMapping("/form")
	public Mono<String> crear(Model model) {
		model.addAttribute("producto", new Producto());
		model.addAttribute("titulo", "Fornulario de Producto");
		return Mono.just("form");
		
	}
	
	@GetMapping("/form/{id}")
	public Mono<String> editar(@PathVariable String id, Model model) {
		Mono<Producto> productoMono = service.findById(id).doOnNext(p -> {
			log.info("Producto: " + p.getNombre());
		}).defaultIfEmpty(new Producto());
		
		model.addAttribute("titulo", "Editar Producto");
		model.addAttribute("producto", productoMono);
		
		return Mono.just("form");
		
	}
	
	@GetMapping("/eliminar/{id}")
	public Mono<String> eliminar(@PathVariable String id) {
		
		return service.findById(id)
				.defaultIfEmpty(new Producto())
				.flatMap(p -> {
					if (p.getId() == null) {
						return Mono.error(new InterruptedException("No existe el producto a eliminar"));
					}
					return Mono.just(p);
				})
				.flatMap(p -> {
					log.info("Eliminando producto: " + p.getNombre());
					log.info("Eliminando producto id: " + p.getId());
			return service.delete(p);
		}).then(Mono.just("redirect:/listar?success=producto+guardado+con+exito"))
		.onErrorResume(ex -> Mono.just("redirect:/listar?error=no+existe+el+producto+a+eliminar"));
	}
	
	@PostMapping("/form")
	public Mono<String> guardar(Producto producto, SessionStatus status) {
		status.setComplete();
		return service.save(producto).doOnNext(p -> {
			log.info("Guardado: " + p.getNombre() + " Id: " + p.getId());
		}).thenReturn("redirect:/listar");
	}
	
	/* 
	   =================================
	   Manejar Contrapresion DATA-DRIVEN
	   =================================
	*/
	@GetMapping("/listar-datadriven")
	public String listarDataDriven(Model model) {
		
		Flux<Producto> productos = service.findAllWithNameUpperCase().delayElements(Duration.ofSeconds(1));
		
		
		productos.subscribe(prod -> log.info(prod.getNombre()));
		
		// Una de las formas de manejar la contrapresion con flux 
		model.addAttribute("productos", new ReactiveDataDriverContextVariable(productos, 1));
		model.addAttribute("titulo", "Listado de Productos");
		log.info("productos" + productos);
		return "listar";
	}
	
	/* 
	   =================================
	   Manejar Contrapresion NORMAL
	   =================================
	*/
	@GetMapping("/listar-full")
	public String listarFull(Model model) {
		
		Flux<Producto> productos = service.findAllWithNameUpperCaseRepeat();
		
		model.addAttribute("productos", productos);
		model.addAttribute("titulo", "Listado de Productos");
		log.info("productos" + productos);
		return "listar";
	}
	
	/* 
	   =================================
	   Manejar Contrapresion CHUNKED
	   =================================
	*/
	@GetMapping("/listar-chunked")
	public String listarChunked(Model model) {
		
		Flux<Producto> productos = service.findAllWithNameUpperCaseRepeat();
		model.addAttribute("productos", productos);
		model.addAttribute("titulo", "Listado de Productos");
		log.info("productos" + productos);
		return "listar-chunked";
	}
	
}
