/*
* [Integrantes e Matricula]
*      Hugo    - 396702   
*      Ricardo - 705069
*/
// BiBliotecas do Java
import java.util.*;
import java.io.*;

//################################################################################################

//Class Data - Para armazenar a data de nascimento
class Data{
  // Variavel
  protected byte dia;
  protected byte mes;
  protected short ano;

  // Construtor I
  public Data(){ 
    this.dia = -1;
    this.mes = -1;
    this.ano = -1;
  }
 
  // Construtor II
  public Data(byte d, byte m, short a){
    this.dia = d;
    this.mes = m;
    this.ano = a;
  }

  //Impressao
  public String toString(){
    return(this.dia + "/" + this.mes + "/" + this.ano);
  }
}

//################################################################################################

// CLASS Prontuario - Para representar um prontuario
class Prontuario{
  // Variavel
  protected char lapide; // Verificador de excluido
  protected String nome; // Nome do cliente
  protected int CPF; // CPF do cliente
  protected char sexo; // Sexo do cliente
  protected Data dataNascimento; // Data de nacimento do Cliente
  protected String anotacoes; // Anotacoes sobre o Cliente
  protected int tamanhoAnotacao; // Tamanho da anotacao presente no prontuario

  // Construtor I
  public Prontuario(int c, String n, char s, String a, Data d, int t){
    this.lapide = ' ';
    this.CPF = c;
    for(int i = 0; i < n.length() && i < 34; i++)
      this.nome += n.charAt(i);
    this.sexo = s;
    this.tamanhoAnotacao = t;
    for(int i = 0; i < a.length() && i < this.tamanhoAnotacao; i++)
      this.anotacoes += a.charAt(i);
    this.dataNascimento = d;
  }
  
  // Construtor II
  public Prontuario(){
    this.lapide = ' ';
    this.CPF = -1;
    this.nome = "";
    this.sexo = ' ';
    tamanhoAnotacao = 0;
    this.anotacoes = "";
    this.dataNascimento = new Data();
  }

  // Mostra que o Prontuario foi excluido
  public void excluir(){
    this.lapide = '*';
  }

  // Mostrar informações do Prontuario
  public void Mostrar(){
    System.out.println("\n|---[ Prontuario ]\n|" + 
            "\n| CPF: " + this.CPF +
            "\n| Nome: " + this.nome + 
            "\n| Sexo: " + this.sexo + 
            "\n| Data de nascimento: " + this.dataNascimento.toString() +
            "\n| Anotações: " + this.anotacoes);
  }
}

//################################################################################################

class ManipuladorDeArquivos {
  // Transforma em binario
  public static int hash(int CPF, int profundidadeGlobal){    
    int binario[] = new int[profundidadeGlobal];    
    int index = 0;
    int resultado = 0;
    while(CPF > 0 && index < profundidadeGlobal){    
      binario[index++] = CPF%2;    
      CPF = CPF/2;    
    }       
    for(int i = 0 ; i < profundidadeGlobal; i++)      
      resultado += binario[i] * Math.pow(2, i);
    return resultado;  
  }

  // Criador de Arquivos
  public void criarArquivos(int profundidadeGlobalInicial, int quantidadeEntradaBucket){
    // Arquivos que vao ser construidos
    FileOutputStream diretorio;
    FileOutputStream indice;
    FileOutputStream arquivoMestre;
    DataOutputStream dos;
    // Variaveis para a construcao
    try { 
      // Criar arquivos
      diretorio = new FileOutputStream("diretorio.db");
      indice = new FileOutputStream("indice.db");
      arquivoMestre = new FileOutputStream("arquivo-mestre.db");
      if(diretorio.getChannel().size() == 0 && indice.getChannel().size() == 0
        && arquivoMestre.getChannel().size() == 0){
        // Colocar quantidade de prontuario no arquivo-mestre.db
        dos = new DataOutputStream(arquivoMestre);
        dos.writeInt(0);
        // Colocar profindidade global inicial
        dos = new DataOutputStream(diretorio);
        dos.writeInt(profundidadeGlobalInicial);
        dos.flush();
        // Criar cestos varios e colocar a profundidade no inicio deles
        dos = new DataOutputStream(indice);
        for(int i = 0; i < Math.pow(2, profundidadeGlobalInicial); i++){
          // Colocar profundidade local
          dos.writeInt(1);
          for(int j = 0; j < quantidadeEntradaBucket; j++){
            // Colocar registro vazio
            dos.writeInt(-1); // CPF
            dos.writeInt(-1); // Local
          }
        }
        dos.flush();
      } else {
        System.out.println("(!) ERRO: Os arquivos contem dados");
      }
    } catch (Exception e) {
      e.printStackTrace();
    } 
  }

  // Inserir no Arquivo
  public void inserirNoArquivo(Prontuario r){
    FileInputStream diretorio;
    FileOutputStream indice;
    FileOutputStream arquivoMestre;
    DataOutputStream out;
    DataInputStream in;
    int valorHash = 0;
    try { 
      // Adicionar dados
      diretorio = new FileInputStream("diretorio.db");
      indice = new FileOutputStream("indice.db", true);
      arquivoMestre = new FileOutputStream("arquivo-mestre.db", true); 
      out = new DataOutputStream(arquivoMestre);
      in  = new DataInputStream(diretorio);
      // Verificar se a chave esta igual
      valorHash = hash(r.CPF, in.readInt());
      // Caminhar nos bucket's
      
      // Incerir dados no arquio-mestre.db
      out.writeChar(r.lapide);
      out.writeInt(r.CPF);
      out.writeUTF(r.nome);
      out.writeChar(r.sexo);
      out.writeByte(r.dataNascimento.dia);
      out.writeByte(r.dataNascimento.mes);
      out.writeShort(r.dataNascimento.ano);
      out.writeInt(r.tamanhoAnotacao);
      out.writeUTF(r.anotacoes);
      out.flush();
    } catch (Exception e) {
      e.printStackTrace();
    } 
  } 
  // Pesquisar 
  public Prontuario pesquisarNoArquivo(){
    FileInputStream diretorio;
    FileInputStream indice;
    FileInputStream arquivoMestre;
    DataInputStream out;
    Prontuario r = new Prontuario();
    try { 
      diretorio = new FileInputStream("diretorio.db");
      indice = new FileInputStream("indice.db");
      arquivoMestre = new FileInputStream("arquivo-mestre.db"); 
      out = new DataInputStream(diretorio);
      r.lapide = out.readChar();
      r.CPF = out.readInt();
      r.nome = out.readUTF();
      r.sexo = out.readChar();
      r.dataNascimento.dia = out.readByte();
      r.dataNascimento.mes = out.readByte();
      r.dataNascimento.ano = out.readShort();
      r.anotacoes = out.readUTF();
    } catch (Exception e) {
      e.printStackTrace();
    } 
    return(r);
  } 
  // Excluir
  // Alterar
}

//################################################################################################

// CLASS MAIN - Para fazer as operacaes e representacoes necessarias 
class Main {
  /* - FUNCOES CRUD - */
  // Inserção
  public static void insercao(Scanner in, ManipuladorDeArquivos M){
    // Definição
    String confirmar = " "; // Confirmador de Inclusão
    Data dataDeNascimento = new Data();
    in.nextLine(); // Limpar entrada
    String linha = "";
    Prontuario r = new Prontuario(); // Criar Prontuario
    // Pegar dados
    // Nome
    while(r.nome.length() <= 0){
      System.out.print("\nColoque o nome (Obs.: Tamanho limite 34 caracteres): ");
      r.nome = in.nextLine();
      if(r.nome.length() <= 0)
          System.out.println("(!) ERRO: O valor passado é invalido");
    }
    // CPF
    while(r.CPF <= 0){
      System.out.print("\nColoque o CPF: ");
      r.CPF = in.nextInt();
      if(r.CPF <= 0)
          System.out.println("(!) ERRO: O valor passado é invalido");
    }
    in.nextLine(); // Tirar '\n
    // Sexo
    while(r.sexo != 'M' && r.sexo != 'F'){
      System.out.print("\nColoque o sexo: ");
      linha = in.nextLine();
      if(linha.length() != 0)
        r.sexo = linha.charAt(0);
      if(r.sexo != 'M' && r.sexo != 'F')
        System.out.println("(!) ERRO: O valor passado é invalido");
    }
    // Data de nascimento
    System.out.print("\n\nData de Nascimento ");
    while(dataDeNascimento.dia <= 0 || dataDeNascimento.dia > 31){
      System.out.print("\n\tColoque o dia: ");
      dataDeNascimento.dia = in.nextByte();
      if(dataDeNascimento.dia <= 0 || dataDeNascimento.dia > 31)
        System.out.println("(!) ERRO: O valor passado é invalido");
    }
    while(dataDeNascimento.mes <= 0 || dataDeNascimento.mes > 12){
      System.out.print("\n\tColoque o mes: ");
      dataDeNascimento.mes = in.nextByte();
      if(dataDeNascimento.mes <= 0 || dataDeNascimento.mes > 12)
        System.out.println("(!) ERRO: O valor passado é invalido");
    }
    System.out.print("\n\tColoque o ano: ");
    dataDeNascimento.ano = (in.nextShort());
    r.dataNascimento = (dataDeNascimento);
    // Tamanho das anotacoes
    System.out.print("\nTamanho limite das anotacoes (em caracteres): ");
    r.tamanhoAnotacao = in.nextInt();
    in.nextLine(); // Tirar '\n'
    // Anotacoes
    System.out.print("\nColoque as anotações (Obs.: Tamanho limite de " + r.tamanhoAnotacao + " caracteres): ");
    r.anotacoes = in.nextLine();
    // Mostrar Prontuario
    r.Mostrar();
    //Pedir confirmacao
    System.out.print("\nINCLUIR esse prontuario?[Y/n] ");
    confirmar = in.nextLine();
    if(confirmar.length() == 0 || (confirmar.charAt(0) == 'y' || confirmar.charAt(0) == 'Y')){
      // Inserir Prontuario
      M.inserirNoArquivo(r);
      System.out.println("\nProntuario foi incluido!");
    } else if (confirmar.charAt(0) == 'n' || confirmar.charAt(0) == 'N'){
      System.out.println("\nProntuario nao foi incluido!");
    } else {
      System.out.println("\nProcedimento abortado!");
    }
    System.out.print("\n\tPressione Enter para continuar");
    in.nextLine(); // Limpar entrada
  } // fim insercao
  
  //----------------------------------------------------------------------------------------------
  
  // Alteracao
  public static void alteracao(Scanner in, ManipuladorDeArquivos M){
    // Definicao
    Prontuario r = new Prontuario(); // Criar Prontuario
    String confirmar = " "; // Confirmador de Execlusao
    in.nextLine(); // Limpar entrada
    // Pesquisar Prontuario
    r = M.pesquisarNoArquivo();
    // Mostrar Prontuario
    r.Mostrar();
    // Pedir confirmacao
    System.out.print("\nALTERAR esse prontuario?[Y/n] ");
    confirmar = in.nextLine();
    if(confirmar.length() == 0 || (confirmar.charAt(0) == 'y' || confirmar.charAt(0) == 'Y')){
      // Pesquisar

    } else if (confirmar.charAt(0) == 'n' || confirmar.charAt(0) == 'N'){
      System.out.println("\nProntuario nao foi excluido!");
    } else {
      System.out.println("\nProcedimento abortado!");
    }
    System.out.print("\n\tPressione Enter para continuar");
    in.nextLine();
  }// fim alteracao

  //----------------------------------------------------------------------------------------------
  
  // Exclusao
  public static void exclusao(int cpf, Scanner in){
    // Definicao
    Prontuario r = new Prontuario(); // Criar Prontuario
    String confirmar = " "; // Confirmador de Execlusao
    in.nextLine(); // Limpar entrada
    // Pesquisar Prontuario

    // Mostrar Prontuario
    r.Mostrar();
    // Pedir confirmacao
    System.out.print("\nEXCLUIR esse prontuario?[Y/n] ");
    confirmar = in.nextLine();
    if(confirmar.length() == 0 || (confirmar.charAt(0) == 'y' || confirmar.charAt(0) == 'Y')){
      r.excluir();
      System.out.println("\nProntuario excluido!");
    } else if (confirmar.charAt(0) == 'n' || confirmar.charAt(0) == 'N'){
      System.out.println("\nProntuario nao foi excluido!");
    } else {
      System.out.println("\nProcedimento abortado!");
    }
    System.out.print("\n\tPressione Enter para continuar");
    in.nextLine();
  }// fim exclusao
  
  //----------------------------------------------------------------------------------------------
  
  // Impressao
  public static void impressao(int cpf, Scanner in){
    // Definicao
    Prontuario r = new Prontuario(); // Criar Prontuario
    in.nextLine(); // Limpar entrada
    // Pesquisar Prontuario

    // Mostrar Prontuario
    r.Mostrar();
    System.out.print("\n\tPressione Enter para continuar");
    in.nextLine();
  }// fim impressao
  
  //----------------------------------------------------------------------------------------------

  /* - Funcao principal - */
  public static void main(String[] args) {
    // Definicao
    int opcao = -1; // Verificador de opcao
    Scanner leitor = new Scanner(System.in); // Leitor de entrada 
    ManipuladorDeArquivos M = new ManipuladorDeArquivos(); // Criar um manipulador
    int profundidadeGlobalInicial = 0;
    int quantidadeEntradaBucket = 0;
    // Mostrar enquanto opcao diferente de 0
    while(opcao != 0){
      // Menu
      System.out.println("\n|---[ MENU ]");
      System.out.println("| Opções:");
      System.out.println("| 0) Sair");
      System.out.println("| 1) Criar arquivo");
      System.out.println("| 2) Insercao");
      System.out.println("| 3) Alteracao");
      System.out.println("| 4) Exclusao");
      System.out.println("| 5) Impressao");
      System.out.println("| 6) Simulação \n|");
      System.out.print("| Escolha: ");
      // Obter escolha
      opcao = leitor.nextInt();
      // Verificar opcao
      switch(opcao){
        case 0:
          System.out.println("\n|---[ Encerrado ]");
          break;
        case 1:
          System.out.println("\n|---[ Criar Arquivo ]");
          while(profundidadeGlobalInicial < 1){
            System.out.print("Coloque a profundidade inicial do diretório: ");
            profundidadeGlobalInicial = leitor.nextInt();
            if(profundidadeGlobalInicial < 1)
              System.out.println("(!) ERRO: O valor passado é invalido");
          }
          while(quantidadeEntradaBucket < 1){
            System.out.print("Coloque o máximo de entradas do bucket: ");
            quantidadeEntradaBucket = leitor.nextInt();
            if(quantidadeEntradaBucket < 1)
              System.out.println("(!) ERRO: O valor passado é invalido");
          }
          M.criarArquivos(profundidadeGlobalInicial, quantidadeEntradaBucket);
          profundidadeGlobalInicial = 0;
          quantidadeEntradaBucket = 0; 
          break;
        case 2:
          System.out.println("\n|---[ Inserir ]");
          insercao(leitor, M);
          break;
        case 3:
          System.out.println("\n|---[ Alterar ]");
          alteracao(leitor, M);
          break;
        case 4:
          System.out.println("\n|---[ Excluir ]");
          exclusao(0, leitor);
          break;
        case 5:
          System.out.println("\n|---[ Imprimir ]");
          impressao(0, leitor);
        case 6:
          System.out.println("\n|---[ Simulacao ]");
          System.out.println("Resultado: " + M.hash(11, 3));
          break;
        default:
          System.out.println("\nERRO: Opcão não encontrada");
      }
    }
    leitor.close();
  } // fim main
} // fim Main

