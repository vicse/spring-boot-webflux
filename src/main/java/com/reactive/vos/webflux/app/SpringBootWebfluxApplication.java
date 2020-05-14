package com.reactive.vos.webflux.app;

import java.util.Date;

import com.reactive.vos.webflux.app.documents.Categoria;
import com.reactive.vos.webflux.app.services.ProductoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import com.reactive.vos.webflux.app.documents.Producto;

import reactor.core.publisher.Flux;

@SpringBootApplication
public class SpringBootWebfluxApplication implements CommandLineRunner {
	
	@Autowired
	private ProductoService service;
	
	@Autowired
	private ReactiveMongoTemplate mongoTemplate;
	
	private static final Logger log = LoggerFactory.getLogger(SpringBootWebfluxApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(SpringBootWebfluxApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		
		mongoTemplate.dropCollection("productos").subscribe();
		mongoTemplate.dropCollection("categorias").subscribe();

		Categoria electronico = new Categoria("Electrónico");
		Categoria deporte = new Categoria("Deporte");
		Categoria computacion = new Categoria("Computación");
		Categoria muebles = new Categoria("Muebles");

		Flux.just(electronico, deporte, computacion, muebles)
		.flatMap(service::saveCategoria)
		.doOnNext(c -> {
			log.info("Categoria creada: " + c.getNombre() + ", Id: " +c.getId());
		}).thenMany(
				Flux.just(new Producto("TV plasma 1", 1232.00, electronico),
								new Producto("Mesa Portable", 1232.00, muebles),
								new Producto("Mouse", 1232.00, electronico),
								new Producto("Readmi note 7", 1232.00, electronico),
								new Producto("Cargador portatil", 1232.00, electronico),
								new Producto("Mochila", 1232.00, deporte)
				)
				.flatMap(producto -> {
					producto.setCreatedAt(new Date());
					return service.save(producto);
				})
		)
		.subscribe(producto -> log.info("Insert: " + producto.getId() + " " + producto.getNombre() ));
		
	}

}
