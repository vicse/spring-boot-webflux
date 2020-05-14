package com.reactive.vos.webflux.app.controllers;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

import com.reactive.vos.webflux.app.documents.Categoria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.thymeleaf.spring5.context.webflux.ReactiveDataDriverContextVariable;

import com.reactive.vos.webflux.app.documents.Producto;
import com.reactive.vos.webflux.app.services.ProductoService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@SessionAttributes("producto")
@Controller
public class ProductoController {
	
	private static final Logger log = LoggerFactory.getLogger(ProductoController.class);
	
	@Autowired
	private ProductoService service;

	@Value("${config.uploads.path}")
	private String path;

	@ModelAttribute("categorias")
	public Flux<Categoria> categorias() {
		return service.findAllCategoria();
	}

	@GetMapping("/uploads/img/{nombreFoto:.+}")
	public Mono<ResponseEntity<Resource>> verFoto(@PathVariable String nombreFoto) throws MalformedURLException {
		Path ruta = Paths.get(path).resolve(nombreFoto).toAbsolutePath();

		Resource imagen = new UrlResource(ruta.toUri());

		return Mono.just(
			ResponseEntity
						.ok()
						.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + imagen.getFilename() + "\"")
						.body(imagen)
		);

	}

	@GetMapping("/ver/{id}")
	public Mono<String> ver(Model model, @PathVariable String id) {
		return service.findById(id)
						.doOnNext(producto -> {
							model.addAttribute("producto", producto);
							model.addAttribute("titulo", "Detalle Producto");
						}).switchIfEmpty(Mono.just(new Producto()))
						.flatMap(producto -> {
							if (producto.getId()==null) {
								return Mono.error(new InterruptedException("No existe el producto"));
							}
							return Mono.just(producto);
						}).then(Mono.just("ver"))
						.onErrorResume(ex -> Mono.just("redirect:/listar?error=no+existe+el+producto"));
	}

	
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
		model.addAttribute("boton", "Crear");
		return Mono.just("form");
		
	}

	@GetMapping("/form-v2/{id}")
	public Mono<String> editarV2(@PathVariable String id, Model model) {

		return service.findById(id).doOnNext(p -> {
			log.info("Producto: " + p.getNombre());
			model.addAttribute("titulo", "Editar Producto");
			model.addAttribute("boton", "Editar");
			model.addAttribute("producto", p);
		}).defaultIfEmpty(new Producto())
		.flatMap(p -> {
			if (p.getId()==null) {
				return Mono.error(new InterruptedException("No existe el producto"));
			}
			return Mono.just(p);
		})
		.then(Mono.just("form"))
		.onErrorResume(ex -> Mono.just("redirect:/listar?error=no+existe+el+producto"));

	}
	
	@GetMapping("/form/{id}")
	public Mono<String> editar(@PathVariable String id, Model model) {
		Mono<Producto> productoMono = service.findById(id).doOnNext(p -> {
			log.info("Producto: " + p.getNombre());
		}).defaultIfEmpty(new Producto());

		model.addAttribute("boton", "Editar");
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
		}).then(Mono.just("redirect:/listar?success=producto+eliminado+con+exito"))
		.onErrorResume(ex -> Mono.just("redirect:/listar?error=no+existe+el+producto+a+eliminar"));
	}
	
	@PostMapping("/form")
	public Mono<String> guardar(@Valid Producto producto, BindingResult result, Model model, @RequestPart FilePart file, SessionStatus status) {

		if (result.hasErrors()) {
			model.addAttribute("titulo", "Errores en el formulario producto");
			model.addAttribute("boton", "Guardar");
			return Mono.just("form");
		} else {
			status.setComplete();

			Mono<Categoria> categoriaMono = service.findByIdCategoria(producto.getCategoria().getId());
			return categoriaMono.flatMap(c -> {
				if (producto.getCreatedAt() == null) {
					producto.setCreatedAt(new Date());
				}

				if (!file.filename().isEmpty()) {
					producto.setFoto(UUID.randomUUID().toString() + "-" + file.filename()
									.replace(" ", "")
									.replace(":", "")
									.replace("\\", "")
					);
				}

				producto.setCategoria(c);
				return  service.save(producto);
			}).doOnNext(p -> {
				log.info("Categoria Guardada: " + p.getCategoria().getNombre() + " Id: " + p.getCategoria().getId());
				log.info("Producto Guardado: " + p.getNombre() + " Id: " + p.getId());
			})
							.flatMap(p -> {
								if (!file.filename().isEmpty()) {
									return file.transferTo(new File(path + p.getFoto()));
								}

								return Mono.empty();
							}).thenReturn("redirect:/listar?success=producto+guardado+con+exito");
		}

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
