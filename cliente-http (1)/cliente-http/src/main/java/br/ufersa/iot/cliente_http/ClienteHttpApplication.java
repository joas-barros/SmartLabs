package br.ufersa.iot.cliente_http;

import br.ufersa.iot.cliente_http.service.ConsultaApiService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tools.jackson.databind.ObjectMapper;

import java.util.Scanner;

@SpringBootApplication
public class ClienteHttpApplication implements CommandLineRunner {

	private final ConsultaApiService api;
	private final ObjectMapper objectMapper;

	public ClienteHttpApplication(ConsultaApiService api) {
		this.api = api;
		this.objectMapper = new ObjectMapper();
	}

	public static void main(String[] args) {
		SpringApplication.run(ClienteHttpApplication.class, args);
	}

	@Override
	public void run(String... args) {
		Scanner scanner = new Scanner(System.in);
		System.out.println("=================================================");
		System.out.println("   CLIENTE HTTP (API GATEWAY) - SMARTLABS CLI    ");
		System.out.println("=================================================");

		while (true) {
			System.out.println("\n=== Menu ===");
			System.out.println("[1]  Status atual de um laboratório (Digital Twins)");
			System.out.println("[2]  Digital Twin de um dispositivo específico");
			System.out.println("[3]  Listar todos os Digital Twins globais");
			System.out.println("[4]  Histórico de um laboratório (Padrão 24h)");
			System.out.println("[5]  Histórico de um dispositivo específico (Padrão 24h)");
			System.out.println("[6]  Estatísticas agregadas de um laboratório");
			System.out.println("[7]  Estado de processamento (médias móveis e padrões)");
			System.out.println("[8]  Painel consolidado (Gateway BFF - Status + Estats)");
			System.out.println("[0]  Sair");
			System.out.print("\nOpção: ");

			String opcao = scanner.nextLine().trim();

			try {
				switch (opcao) {
					case "1" -> imprimir(api.statusLaboratorio(lerLab(scanner)));
					case "2" -> {
						String lab = lerLab(scanner);
						System.out.print("ID do dispositivo (ex: PC01): ");
						String disp = scanner.nextLine().trim().toUpperCase();
						imprimir(api.digitalTwin(lab, disp));
					}
					case "3" -> imprimir(api.listarTwins());
					case "4" -> imprimir(api.historicoLaboratorio(lerLab(scanner), lerIntervalo(scanner)));
					case "5" -> {
						String lab = lerLab(scanner);
						System.out.print("ID do dispositivo (ex: PC01): ");
						String disp = scanner.nextLine().trim().toUpperCase();
						imprimir(api.historicoDispositivo(lab, disp, lerIntervalo(scanner)));
					}
					case "6" -> imprimir(api.estatisticasLaboratorio(lerLab(scanner), lerIntervalo(scanner)));
					case "7" -> imprimir(api.processamentoLaboratorio(lerLab(scanner)));
					case "8" -> imprimir(api.painelLaboratorio(lerLab(scanner)));
					case "0" -> {
						System.out.println("Encerrando CLI...");
						System.exit(0); // Garante que o Spring Context é destruído
					}
					default  -> System.out.println("Opção inválida.");
				}
			} catch (ConsultaApiService.ConsultaException e) {
				System.out.println("❌ Erro: " + e.getMessage());
			}
		}
	}

	private String lerLab(Scanner scanner) {
		System.out.print("Laboratório (ex: LAB1, LAB2): ");
		String lab = scanner.nextLine().trim().toUpperCase();
		return lab.isBlank() ? "LAB1" : lab;
	}

	private String lerIntervalo(Scanner scanner) {
		System.out.print("Intervalo (ex: 1h, 30m) [Enter para 24h]: ");
		String v = scanner.nextLine().trim();
		return v.isBlank() ? "24h" : v;
	}

	private void imprimir(String jsonBruto) {
		System.out.println("\n--- Resposta da API Gateway ---");
		try {
			// Tenta dar Pretty Print no JSON para ficar bonito no terminal
			Object json = objectMapper.readValue(jsonBruto, Object.class);
			System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json));
		} catch (Exception e) {
			// Fallback para impressão bruta se não for um JSON válido
			System.out.println(jsonBruto);
		}
		System.out.println("-------------------------------");
	}
}
