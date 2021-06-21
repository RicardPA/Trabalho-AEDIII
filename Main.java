//################################################################################################
/*
 * [Integrantes e Matricula]
 *      Hugo    - 396702
 *      Ricardo - 705069
 */
//################################################################################################

// BiBliotecas do Java
import java.io.*;
import java.util.*;
import java.lang.*;


//################################################################################################

//CLASS Data - Para armazenar a data de nascimento
class Data {
  // Variavel
  protected byte dia;
  protected byte mes;
  protected short ano;

  // Construtor I
  public Data() {
    this.dia = -1;
    this.mes = -1;
    this.ano = -1;
  }

  // Construtor II
  public Data(byte d, byte m, short a) {
    this.dia = d;
    this.mes = m;
    this.ano = a;
  }


  //Impressao
  public String toString() {
    return (this.dia + "/" + this.mes + "/" + this.ano);
  }
}

//################################################################################################

// CLASS Prontuario - Para representar um prontuario
class Prontuario {
  // Variavel
  protected char lapide; // Verificador de excluido
  protected String nome; // Nome do cliente
  protected int CPF; // CPF do cliente
  protected char sexo; // Sexo do cliente
  protected Data dataNascimento; // Data de nacimento do Cliente
  protected String anotacoes; // Anotacoes sobre o Cliente
  protected int tamanhoAnotacao; // Tamanho da anotacao presente no prontuario
  protected int tamanhoProntuario; // Tamanho do prontuario

  // Construtor I
  public Prontuario(int c, String n, char s, String a, Data d, int t, int tp) {
    this.lapide = ' ';
    this.CPF = c;
    this.nome = "";
    for (int i = 0; i < 34; i++) {
      if (i < n.length())
        this.nome += n.charAt(i);
      else
        this.nome += " ";
    }
    this.sexo = s;
    this.tamanhoAnotacao = t;
    this.anotacoes = "";
    for (int i = 0; i < a.length() && i < this.tamanhoAnotacao; i++)
      this.anotacoes += a.charAt(i);
    this.dataNascimento = d;
    this.tamanhoProntuario = tp;
  }

  // Construtor II
  public Prontuario() {
    this.lapide = ' ';
    this.CPF = -1;
    this.nome = "";
    this.sexo = ' ';
    tamanhoAnotacao = 0;
    this.anotacoes = "";
    this.dataNascimento = new Data();
    this.tamanhoProntuario = -1;
  }

  // Mostra que o Prontuario foi excluido
  public void excluir() {
    this.lapide = '*';
  }

  // Mostrar informacoes do Prontuario
  public void Mostrar() {
    System.out.println("\n|---[ Prontuario ]\n|" +
            "\n| CPF: " + this.CPF +
            "\n| Nome: " + this.nome +
            "\n| Sexo: " + this.sexo +
            "\n| Data de nascimento: " + this.dataNascimento.toString() +
            "\n| Anotacoes: " + this.anotacoes);
  }

  // Entregar o tamanho do Prontuario
  public int getTamanhoProntuario(){
    int tamanho = 0;
    try{
      byte[] getBytes = this.anotacoes.getBytes("UTF-8");
      tamanho = 56 + getBytes.length + 2*(this.tamanhoAnotacao - this.anotacoes.length()) + 2;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return(tamanho);
  }
}

//################################################################################################

//CLASS ManipuladorDeArquivos - Para fazer o CRUD dos Prontuarios
class ManipuladorDeArquivos {

  // hash - Calcular o valor do hash
  public static int hash(int CPF, int profundidadeGlobal) {
    return ((int) (CPF % (Math.pow(2, profundidadeGlobal))));
  }

  // criarArquivos - Criar todos arquivos que serao utilizados
  public void criarArquivos(int profundidadeGlobalInicial, int quantidadeEntradaBucket) {
    // Arquivos que vao ser construidos
    FileOutputStream diretorio;
    FileOutputStream indice;
    FileOutputStream arquivoMestre;
    DataOutputStream dos;
    try {
      // Criar arquivos
      diretorio = new FileOutputStream("diretorio.db");
      indice = new FileOutputStream("indice.db");
      arquivoMestre = new FileOutputStream("arquivo-mestre.db");
      // Colocar quantidade de prontuario no arquivo-mestre.db
      dos = new DataOutputStream(arquivoMestre);
      dos.writeInt(0);
      dos.flush();
      // Criar cestos varios e colocar a profundidade no inicio deles
      dos = new DataOutputStream(indice);
      // Colocar a quantidade de entradas de cada Bucket no inicio
      dos.writeInt(quantidadeEntradaBucket);
      for (int i = 0; i < Math.pow(2, profundidadeGlobalInicial); i++) {
        // Colocar profundidade local inicial
        dos.writeInt(profundidadeGlobalInicial);
        for (int j = 0; j < quantidadeEntradaBucket; j++) {
          // Colocar registro vazio
          dos.writeInt(-1); // CPF
          dos.writeInt(-1); // Local
        }
      }
      dos.flush();
      // Colocar profindidade global inicial
      dos = new DataOutputStream(diretorio);
      dos.writeInt(profundidadeGlobalInicial);
      // Colocar ponteiro e sua posicao
      for (int i = 0; i < Math.pow(2, profundidadeGlobalInicial); i++) {
        dos.writeInt(profundidadeGlobalInicial);
        dos.writeInt(i);
        if (i > 0)
          dos.writeInt((i * (quantidadeEntradaBucket * 2) + (i)));
        else
          dos.writeInt(0);
      }
      dos.flush();
      diretorio.close();
      indice.close();
      arquivoMestre.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // inserirNoArquivo - Inserir prontuarios no arquivo-mestre e 
  // organizar o diretório e indice
  public boolean inserirNoArquivo(Prontuario r) {
    // Metodos para leitura e escrita
    DataOutputStream out;
    DataInputStream in;
    // Varianveis nescessarias
    boolean jaExite = false;
    boolean inserido = false;
    try {
      // Conexao com arquivos
      FileInputStream lerInsercao = new FileInputStream("diretorio.db");
      FileOutputStream escreverInsercao = new FileOutputStream("arquivo-mestre.db", true);
      RandomAccessFile crudInsercao = new RandomAccessFile("indice.db", "rw");
      while (!inserido && !jaExite) {
        boolean posicaoLivre = false;
        int valorHash = 0;
        int valorHashNoArquivo = 0;
        int profundidadeGlobal = 0;
        int profundidadeLocal = 0;
        int quantidadeDeEntradaBuckets = 0;
        int quantidadeProntuarios = 0;
        int posicaoNovoProntuario = 0;
        int posicaoNoIndice = -1;
        int posicaoNoIndiceC = -1;
        int posicaoLivreInt = 0;
        int ponteiroLeitor = 0;
        int quantPront = 0;
        int posicaoUltimoBucket = 0;
        int verificadorInt = 0;
        int verificadorPonteiro = 4;
        char verificadorChar;
        int verificadorPosicao = 4;
        boolean verificadorBool =  false;
        int verificadorProntuarioPosicao = 0;
        // Conectar
        crudInsercao = new RandomAccessFile("indice.db", "rw");
        // Pegar quantidade de entrada de cada Buckets
        crudInsercao.seek(0);
        quantidadeDeEntradaBuckets = crudInsercao.readInt();
        // Pegar profundidade global
        lerInsercao = new FileInputStream("diretorio.db");
        in = new DataInputStream(lerInsercao);
        profundidadeGlobal = in.readInt(); // profundidade global
        // Caminhar nos buckets
        valorHash = hash(r.CPF, profundidadeGlobal);
        for (int i = 0; i < Math.pow(2, profundidadeGlobal) && posicaoNoIndice < 0; i++) {
          in.readInt(); // Pular um valor
          int x = in.readInt();
          if (valorHash == x) {
            posicaoNoIndice = in.readInt(); //Posicao no indice
            valorHashNoArquivo = valorHash;
          } else
            in.readInt(); // Pular uma posicao
        }
        // Pegar a profundidade local
        crudInsercao.seek(0);
        crudInsercao.seek((4 * posicaoNoIndice) + 4);
        profundidadeLocal = crudInsercao.readInt();
        // Verificar se ha posicao livre
        for (posicaoLivreInt = 0; posicaoLivreInt <= (quantidadeDeEntradaBuckets * 8)
                && !posicaoLivre; posicaoLivreInt += 8) {
          crudInsercao.seek(0);
          crudInsercao.seek(4 + (posicaoNoIndice * 4 + posicaoLivreInt) - 4);
          if(crudInsercao.readInt() == r.CPF){
            posicaoLivre = true;
            jaExite = true;
          }
          if (crudInsercao.readInt() == -1)
            posicaoLivre = true;
        }
        posicaoLivreInt -= 8;       
        if (posicaoLivre && !jaExite) {
          // Confirmar configuraçao do prontuario
          r = new Prontuario(r.CPF, r.nome, r.sexo, r.anotacoes, 
              r.dataNascimento, r.tamanhoAnotacao, r.getTamanhoProntuario());
          lerInsercao = new FileInputStream("arquivo-mestre.db");
          in = new DataInputStream(lerInsercao);
          quantidadeProntuarios = in.readInt();
          // Encontrar arquivo excluido que pode ser sobrescrito
          crudInsercao = new RandomAccessFile("arquivo-mestre.db", "rw");
          crudInsercao.seek(verificadorPonteiro);
          while(crudInsercao.getFilePointer() < crudInsercao.length() && !verificadorBool){
            verificadorInt = crudInsercao.readInt();
            verificadorChar = crudInsercao.readChar();
            if(verificadorInt == r.tamanhoProntuario && verificadorChar == '*'){
              verificadorPosicao = ((int)crudInsercao.getFilePointer()) - 6;
              r.tamanhoProntuario = verificadorInt;
              verificadorBool = true;
            }
            verificadorProntuarioPosicao++;
            verificadorPonteiro += verificadorInt;
            crudInsercao.seek(verificadorPonteiro);
          }
          // - - -
          // Colocar CPF e posicao
          crudInsercao = new RandomAccessFile("indice.db", "rw");
          crudInsercao.seek(0);
          crudInsercao.seek(posicaoNoIndice * 4 + (posicaoLivreInt));
          crudInsercao.writeInt(r.CPF);
          crudInsercao.seek(posicaoNoIndice * 4 + (posicaoLivreInt) + 4);
          if(verificadorBool)
            crudInsercao.writeInt(verificadorProntuarioPosicao);
          else
            crudInsercao.writeInt(quantidadeProntuarios);
          // Incerir dados no arquio-mestre.db
          if(verificadorBool){
            crudInsercao = new RandomAccessFile("arquivo-mestre.db", "rw");
            crudInsercao.seek(verificadorPosicao);
            crudInsercao.writeInt(r.tamanhoProntuario);
            crudInsercao.writeChar(r.lapide);
            crudInsercao.writeInt(r.CPF);
            crudInsercao.writeUTF(r.nome);
            crudInsercao.writeChar(r.sexo);
            crudInsercao.writeByte(r.dataNascimento.dia);
            crudInsercao.writeByte(r.dataNascimento.mes);
            crudInsercao.writeShort(r.dataNascimento.ano);
            crudInsercao.writeInt(r.tamanhoAnotacao);
            crudInsercao.writeUTF(r.anotacoes);
            for(int i = r.anotacoes.length(); i < r.tamanhoAnotacao; i++)
              crudInsercao.writeChar(' ');
          } else{
            escreverInsercao = new FileOutputStream("arquivo-mestre.db", true);
            out = new DataOutputStream(escreverInsercao);
            out.writeInt(r.getTamanhoProntuario());
            out.writeChar(r.lapide);
            out.writeInt(r.CPF);
            out.writeUTF(r.nome);
            out.writeChar(r.sexo);
            out.writeByte(r.dataNascimento.dia);
            out.writeByte(r.dataNascimento.mes);
            out.writeShort(r.dataNascimento.ano);
            out.writeInt(r.tamanhoAnotacao);
            out.writeUTF(r.anotacoes);
            for(int i = r.anotacoes.length(); i < r.tamanhoAnotacao; i++)
              out.writeChar(' ');
            out.flush();
            crudInsercao = new RandomAccessFile("arquivo-mestre.db", "rw");
            crudInsercao.seek(0);
            crudInsercao.writeInt(quantidadeProntuarios + 1);
          }
          inserido = true;
        } else if(!jaExite){
          // Alterar o diretorio
          crudInsercao = new RandomAccessFile("diretorio.db", "rw");
          crudInsercao.seek(0);
          profundidadeGlobal = crudInsercao.readInt();
          if (profundidadeLocal == profundidadeGlobal) {
            // Fazer copia no final do arquivo
            for (int i = 4; i <= (Math.pow(2, profundidadeGlobal) * 12); i += 12) {
              crudInsercao.seek(0);
              crudInsercao.seek(i);
              ponteiroLeitor = crudInsercao.readInt();
              crudInsercao.seek(0);
              crudInsercao.seek((int) (Math.pow(2, profundidadeGlobal) * 12 + i));
              crudInsercao.writeInt(ponteiroLeitor);
              // - - -
              crudInsercao.seek(0);
              crudInsercao.seek(i + 4);
              ponteiroLeitor = crudInsercao.readInt();
              crudInsercao.seek(0);
              crudInsercao.seek(((int) (Math.pow(2, profundidadeGlobal) * 12 + i) + 4));
              crudInsercao.writeInt(ponteiroLeitor + ((int) Math.pow(2, profundidadeGlobal)));
              // - - -
              crudInsercao.seek(0);
              crudInsercao.seek(i + 8);
              ponteiroLeitor = crudInsercao.readInt();
              crudInsercao.seek(0);
              crudInsercao.seek(((int) (Math.pow(2, profundidadeGlobal) * 12 + i) + 8));
              crudInsercao.writeInt(ponteiroLeitor);
            }
            // Colocar nova profundidade global no arquivo
            crudInsercao.seek(0);
            profundidadeGlobal += 1;
            crudInsercao.writeInt(profundidadeGlobal);
          }
          profundidadeLocal += 1;
          // Obter posicao do ultimo buket
          for (int i = 12; i <= (Math.pow(2, profundidadeGlobal) * 12); i += 12) {
            crudInsercao.seek(0);
            crudInsercao.seek(i);
            ponteiroLeitor = crudInsercao.readInt();
            if (ponteiroLeitor > posicaoUltimoBucket) {
              posicaoUltimoBucket = ponteiroLeitor;
            }
          }
          // Alterar a profundidade local e posicao
          for (int i = 12; i <= (Math.pow(2, profundidadeGlobal) * 12) && quantPront <= 1; i += 12) {
            crudInsercao.seek(0);
            crudInsercao.seek(i);
            ponteiroLeitor = crudInsercao.readInt();
            if (ponteiroLeitor == posicaoNoIndice && quantPront == 0) {
              crudInsercao.seek(i - 8);
              crudInsercao.writeInt(profundidadeLocal);
              valorHashNoArquivo = crudInsercao.readInt();
              quantPront++;
            } else if (ponteiroLeitor == posicaoNoIndice) {
              crudInsercao.seek(i - 8);
              crudInsercao.writeInt(profundidadeLocal);
              crudInsercao.seek(i);
              crudInsercao.writeInt((posicaoUltimoBucket + quantidadeDeEntradaBuckets * 2 + 1));
            }
          }
          quantPront = 0;
          posicaoNoIndiceC = (posicaoUltimoBucket + quantidadeDeEntradaBuckets * 2 + 1);
          // Alterar o indice
          crudInsercao = new RandomAccessFile("indice.db", "rw");
          // Colocar nova profundidade local no Indice
          crudInsercao.seek(0);
          crudInsercao.seek((4 * posicaoNoIndice) + 4);
          crudInsercao.writeInt(profundidadeLocal);
          crudInsercao.seek(0);
          crudInsercao.seek(crudInsercao.length());
          crudInsercao.writeInt(profundidadeLocal);
          for (int i = 0; i < quantidadeDeEntradaBuckets; i++) {
            crudInsercao.writeInt(-1);
            crudInsercao.writeInt(-1);
          }
          // Preparar para o rehash
          int[] arrayComOsValores = new int[quantidadeDeEntradaBuckets * 2];
          crudInsercao.seek(0);
          crudInsercao.seek((4 * posicaoNoIndice) + 8);
          for (int i = 0; i < (quantidadeDeEntradaBuckets * 2); i += 2) {
            arrayComOsValores[i] = crudInsercao.readInt();
            arrayComOsValores[i + 1] = crudInsercao.readInt();
          }
          crudInsercao.seek(0);
          crudInsercao.seek((4 * posicaoNoIndice) + 8);
          for (int i = 0; i < (quantidadeDeEntradaBuckets * 2); i++) {
            crudInsercao.writeInt(-1);
          }
          // Fazer Rehash
          for (int i = 0; i < (quantidadeDeEntradaBuckets * 2); i += 2) {
            crudInsercao.seek(0);
            if (hash(arrayComOsValores[i], profundidadeGlobal) == valorHashNoArquivo)
              ponteiroLeitor = ((4 * posicaoNoIndice) + 4);
            else
              ponteiroLeitor = ((4 * posicaoNoIndiceC) + 4);
            for (int j = 0; j < (quantidadeDeEntradaBuckets * 2); j += 2) {
              if((arrayComOsValores[i] != -1)){
                crudInsercao.seek(0);
                crudInsercao.seek(ponteiroLeitor + (j * 4) + 4);
                if (crudInsercao.readInt() == -1) {
                  crudInsercao.seek(ponteiroLeitor + (j * 4) + 4);
                  crudInsercao.writeInt(arrayComOsValores[i]);
                  crudInsercao.writeInt(arrayComOsValores[i + 1]);
                  arrayComOsValores[i] = -1;
                  arrayComOsValores[i + 1] = -1;
                } else
                  crudInsercao.readInt(); // pular linha
              }
            }
          }
        }
      }
      crudInsercao.close();
      escreverInsercao.close();
      lerInsercao.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return(inserido);
  }

  // Alterar - Encontrar o arquivo e Alterar o campo de anotacoes
  public void alterarArquivo(int cpf, Scanner inScanner) {
    // Conexao com arquivos
    FileInputStream lerAlterar;
    RandomAccessFile crudAlterar;
    // Metodos para leitura e escrita
    DataInputStream in;
    // Variaveis
    Prontuario r = new Prontuario();
    String linha;
    String linhaOffice;
    String confirmar;
    boolean result = false;
    int quantidadeDeEntradaBuckets = 0;
    int profundidadeGlobal = 0;
    int posicaoNoIndice = -1;
    int posicaoPonteiro = 0;
    int posicaoDoProntuario = 0;
    int valorHash = 0;
    int posicaoReal;
    long tempo = 0;
    inScanner.nextLine();
    try {
      tempo = System.currentTimeMillis();
      // Obter quantidade de entrada
      crudAlterar = new RandomAccessFile("indice.db", "rw");
      crudAlterar.seek(0);
      quantidadeDeEntradaBuckets = crudAlterar.readInt();
      // Encontrar posiçao no Indice
      lerAlterar = new FileInputStream("diretorio.db");
      in = new DataInputStream(lerAlterar);
      profundidadeGlobal = in.readInt(); // profundidade global
      // Caminhar nos buckets
      valorHash = hash(cpf, profundidadeGlobal);
      for (int i = 0; i < Math.pow(2, profundidadeGlobal) && posicaoNoIndice < 0; i++) {
        in.readInt(); // Pular uma posicao
        if (valorHash == in.readInt()) {
          posicaoNoIndice = in.readInt(); //Posicao no indice
        } else
          in.readInt(); // Pular uma posicao
      }
      // Encontrar a posicao no Prontuario
      for (posicaoPonteiro = 0; posicaoPonteiro < (quantidadeDeEntradaBuckets * 8)
              && !result; posicaoPonteiro += 4) {
        crudAlterar.seek(0);
        crudAlterar.seek(8 + posicaoNoIndice * 4 + posicaoPonteiro);
        if (crudAlterar.readInt() == cpf) {
          crudAlterar.seek(12 + posicaoNoIndice * 4 + posicaoPonteiro);
          posicaoDoProntuario = crudAlterar.readInt();
          result = true;
        }
      }
      if (result) {
        crudAlterar = new RandomAccessFile("arquivo-mestre.db", "rw");
        crudAlterar.seek(0);
        posicaoReal = 4;
        for(int i = posicaoDoProntuario; i > 0 ; i--){
          crudAlterar.seek(posicaoReal);
          posicaoReal += crudAlterar.readInt();
        }
        crudAlterar.seek(posicaoReal);
        r.tamanhoProntuario = crudAlterar.readInt();
        r.lapide = crudAlterar.readChar();
        r.CPF = crudAlterar.readInt();
        r.nome = crudAlterar.readUTF();
        r.sexo = crudAlterar.readChar();
        r.dataNascimento.dia = crudAlterar.readByte();
        r.dataNascimento.mes = crudAlterar.readByte();
        r.dataNascimento.ano = crudAlterar.readShort();
        r.tamanhoAnotacao = crudAlterar.readInt();
        r.anotacoes = crudAlterar.readUTF();
        r.Mostrar();
        // Pedir confirmacao
        System.out.println("\n Tempo da pesquisa: " + ((System.currentTimeMillis() - tempo)) + "ms");
        System.out.print("\nALTERAR esse prontuario?[Y/n] ");
        confirmar = inScanner.nextLine();
        if (confirmar.length() == 0 || (confirmar.charAt(0) == 'y' || confirmar.charAt(0) == 'Y')) {
          System.out.println("Nova anotaçao (Obs.: Tamanho limite de " + r.tamanhoAnotacao + " caracteres): ");
          linha = inScanner.nextLine();
          r.anotacoes = "";
          for (int i = 0; i < (r.tamanhoAnotacao); i++){
            if(i < linha.length())
              r.anotacoes += linha.charAt(i);
            else
              r.anotacoes += " ";
          }
          r.Mostrar();
          crudAlterar.seek(posicaoReal+56);
          crudAlterar.writeUTF(r.anotacoes);
        } else if (confirmar.charAt(0) == 'n' || confirmar.charAt(0) == 'N') {
          System.out.println("\nProntuario nao foi excluido!");
        } else {
          System.out.println("\nProcedimento abortado!");
        }
      } else {
        System.out.println("(!) ERRO: O prontuario nao existe");
      }
      System.out.print("\n\tPressione Enter para continuar");
      inScanner.nextLine();
      crudAlterar.close();
      lerAlterar.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // Excluir - Fazer a exclusão logica do arquivo mestre e
  // organizar o indice
  public boolean excluirProntuario(int cpf) {
    // Conexao com arquivos
    FileInputStream lerExcluir;
    RandomAccessFile crudExcluir;
    // Metodos para leitura e escrita
    DataInputStream in; 
    // Variaveis
    boolean result = false;
    int quantidadeDeEntradaBuckets = 0;
    int profundidadeGlobal = 0;
    int posicaoNoIndice = -1;
    int posicaoPonteiro = 0;
    int posicaoDoProntuario = 0;
    int valorHash = 0;
    int posicaoReal;
    try {
      // Obter quantidade de entrada
      crudExcluir = new RandomAccessFile("indice.db", "rw");
      crudExcluir.seek(0);
      quantidadeDeEntradaBuckets = crudExcluir.readInt();
      // Encontrar posiçao no Indice
      lerExcluir = new FileInputStream("diretorio.db");
      in = new DataInputStream(lerExcluir);
      profundidadeGlobal = in.readInt(); // profundidade global
      // Caminhar nos buckets
      valorHash = hash(cpf, profundidadeGlobal);
      for (int i = 0; i < Math.pow(2, profundidadeGlobal) && posicaoNoIndice < 0; i++) {
        in.readInt(); // Pular uma posicao
        if (valorHash == in.readInt()) {
          posicaoNoIndice = in.readInt(); //Posicao no indice
        } else
          in.readInt(); // Pular uma posicao
      }
      // Encontrar a posicao no Prontuario
      for (posicaoPonteiro = 0; posicaoPonteiro < (quantidadeDeEntradaBuckets * 8)
              && !result; posicaoPonteiro += 4) {
        crudExcluir.seek(0);
        crudExcluir.seek(8 + posicaoNoIndice * 4 + posicaoPonteiro);
        if (crudExcluir.readInt() == cpf) {
          crudExcluir.seek(8 + posicaoNoIndice * 4 + posicaoPonteiro);
          crudExcluir.writeInt(-1);
          posicaoDoProntuario = crudExcluir.readInt();
          crudExcluir.seek(12 + posicaoNoIndice * 4 + posicaoPonteiro);
          crudExcluir.writeInt(-1);
          result = true;
        }
      }
      if (result) {
        crudExcluir = new RandomAccessFile("arquivo-mestre.db", "rw");
        crudExcluir.seek(0);
        posicaoReal = 4;
        for(int i = posicaoDoProntuario; i > 0 ; i--){
          crudExcluir.seek(posicaoReal);
          posicaoReal += crudExcluir.readInt();
        }
        crudExcluir.seek(posicaoReal + 4);
        crudExcluir.writeChar('*');
      }
      crudExcluir.close();
      lerExcluir.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return (result);
  }

  // mostrarArquivos - mostrar os arquivos que estão sendo utilizados 
  // (indice, arquivo mestre e o diretorio)
  public void mostrarArquivos(Scanner in) {
    RandomAccessFile crudMostrar;
    in.nextLine();
    int quantEntr = 0;
    int tamanhoProntuario = 0;
    int tamanhoAnotacao = 0;
    String anotacoes = "";
    try {
      // Ler diretorio
      crudMostrar = new RandomAccessFile("diretorio.db", "rw");
      crudMostrar.seek(0);
      System.out.println("|--|DIRETORIO|\n|");
      System.out.println("| Profundidade Global: " + crudMostrar.readInt());
      System.out.println("|\n_______________\n|");
      while(crudMostrar.getFilePointer() < crudMostrar.length()){
        System.out.println("| Profundidade Local: " + crudMostrar.readInt());
        System.out.println("| Valor do Local: " + crudMostrar.readInt());
        System.out.println("| Posicao no Indice: " + crudMostrar.readInt());
        if(crudMostrar.getFilePointer() < crudMostrar.length())
          System.out.println("|\n_______________\n|");
        else
          System.out.println("|\n_______________\n");
      }
      System.out.print("\n\tPressione Enter para mostrar proximo arquivo");
      in.nextLine();
      // Ler indice
      crudMostrar = new RandomAccessFile("indice.db", "rw");
      crudMostrar.seek(0);
      System.out.println("\n|--|Indice|\n|");
      quantEntr = crudMostrar.readInt();
      System.out.println("| Quantidade de entradas de cada Bucket: " + quantEntr);
      System.out.println("|\n_______________\n|");
      while(crudMostrar.getFilePointer() < crudMostrar.length()){
        System.out.println("| Profundidade Local: " + crudMostrar.readInt());
        for(int i = 0; i < quantEntr; i++){
          System.out.println("| Valor do CPF: " + crudMostrar.readInt());
          System.out.println("| Posicao no Arquivo Mestre: " + crudMostrar.readInt());
        }
        if(crudMostrar.getFilePointer() < crudMostrar.length())
          System.out.println("|\n_______________\n|");
        else
          System.out.println("|\n_______________\n");
      }
      System.out.print("\n\tPressione Enter para mostrar proximo arquivo");
      in.nextLine();
      // ler arquivo mestre
      crudMostrar = new RandomAccessFile("arquivo-mestre.db", "rw");
      crudMostrar.seek(0);
      System.out.println("\n|--|ARQUIVO MESTRE|\n|");
      System.out.println("| Quantidade de Prontuario: " + crudMostrar.readInt());
      System.out.println("|\n_______________\n|");
      while(crudMostrar.getFilePointer() < crudMostrar.length()){
        crudMostrar.readInt();
        System.out.println("| Lapide: " + crudMostrar.readChar());
        System.out.println("| Valor do CPF: " + crudMostrar.readInt());
        System.out.println("| Nome: " + crudMostrar.readUTF());
        System.out.println("| Sexo: " + crudMostrar.readChar());
        System.out.println("| Data: " + crudMostrar.readByte() + "/" + crudMostrar.readByte() + "/" + crudMostrar.readShort());
        tamanhoAnotacao = crudMostrar.readInt();
        anotacoes = crudMostrar.readUTF();
        System.out.println("| Anotacoes: " + anotacoes);
        for(int i = anotacoes.length(); i < tamanhoAnotacao; i++)
            crudMostrar.readChar();
        if(crudMostrar.getFilePointer() < crudMostrar.length())
          System.out.println("|\n_______________\n|");
        else
          System.out.println("|\n_______________\n");
      }
      crudMostrar.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // Recuperar - Encontrar e pegar os valores do prontuario
  // utilizando os arquivos (Obs.: Metodo utilizado na Simulacao)
  public boolean Recuperar(int cpf){
    // Conexao com arquivos
    FileInputStream lerRecuperar;
    RandomAccessFile crudRecuperar;
    // Metodos para leitura e escrita
    Prontuario r = new Prontuario();
    DataInputStream in;
    // Variaveis
    boolean result = false;
    int quantidadeDeEntradaBuckets = 0;
    int profundidadeGlobal = 0;
    int posicaoNoIndice = -1;
    int posicaoPonteiro = 0;
    int posicaoDoProntuario = 0;
    int valorHash = 0;
    int posicaoReal;
    try {
      // Obter quantidade de entrada
      crudRecuperar = new RandomAccessFile("indice.db", "rw");
      crudRecuperar.seek(0);
      quantidadeDeEntradaBuckets = crudRecuperar.readInt();
      // Encontrar posiçao no Indice
      lerRecuperar = new FileInputStream("diretorio.db");
      in = new DataInputStream(lerRecuperar);
      profundidadeGlobal = in.readInt(); // profundidade global
      // Caminhar nos buckets
      valorHash = hash(cpf, profundidadeGlobal);
      for (int i = 0; i < Math.pow(2, profundidadeGlobal) && posicaoNoIndice < 0; i++) {
        in.readInt(); // Pular uma posicao
        if (valorHash == in.readInt()) {
          posicaoNoIndice = in.readInt(); //Posicao no indice
        } else
          in.readInt(); // Pular uma posicao
      }
      // Encontrar a posicao no Prontuario
      for (posicaoPonteiro = 0; posicaoPonteiro < (quantidadeDeEntradaBuckets * 8)
              && !result; posicaoPonteiro += 4) {
        crudRecuperar.seek(0);
        crudRecuperar.seek(8 + posicaoNoIndice * 4 + posicaoPonteiro);
        if (crudRecuperar.readInt() == cpf) {
          crudRecuperar.seek(12 + posicaoNoIndice * 4 + posicaoPonteiro);
          posicaoDoProntuario = crudRecuperar.readInt();
          result = true;
        }
      }
      if (result) {
        crudRecuperar = new RandomAccessFile("arquivo-mestre.db", "rw");
        crudRecuperar.seek(0);
        posicaoReal = 4;
        for(int i = posicaoDoProntuario; i > 0 ; i--){
          crudRecuperar.seek(posicaoReal);
          posicaoReal += crudRecuperar.readInt();
        }
        crudRecuperar.seek(posicaoReal);
        r.tamanhoProntuario = crudRecuperar.readInt();
        r.lapide = crudRecuperar.readChar();
        r.CPF = crudRecuperar.readInt();
        r.nome = crudRecuperar.readUTF();
        r.sexo = crudRecuperar.readChar();
        r.dataNascimento.dia = crudRecuperar.readByte();
        r.dataNascimento.mes = crudRecuperar.readByte();
        r.dataNascimento.ano = crudRecuperar.readShort();
        r.tamanhoAnotacao = crudRecuperar.readInt();
        r.anotacoes = crudRecuperar.readUTF();
      }
      crudRecuperar.close();
      lerRecuperar.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return(result);
  }

  // Simulacao - Fazer a insercao e recuperacao de varios arquivos, 
  // com variacao de chave (CPF), tamanho da anotacao, profundidade global
  // e a quntidade de entrada por bucket
  public void Simulacao() {
    RandomAccessFile crudSimulacao;
    long tempoInserir;
    Long tempoRecuperar;
    int tamanhoAnotacao = 10;
    Prontuario r = new Prontuario();
    Data d = new Data((byte)10,(byte)10,(short)2001);
    try{
      crudSimulacao = new RandomAccessFile("arquivo-mestre.db", "rw");
      // Primeira Simulacao
      tamanhoAnotacao = 100000;
      criarArquivos(1, 1024);
      System.out.println("\n| Inserir...");
      tempoInserir = System.currentTimeMillis();
      for(int j = 1; j <= (5550); j++){
        if(j%370 == 0)
          System.out.println("(!) Quantidade de Prontuarios Incluidos: " + j);
        r.CPF = j;
        r.nome = ("Prontuario " + j);
        r.sexo = 'F'; 
        r.dataNascimento = d;
        r.tamanhoAnotacao = tamanhoAnotacao;
        r.anotacoes = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        r.tamanhoProntuario = r.getTamanhoProntuario();
        inserirNoArquivo(r);
      }
      tempoInserir = (System.currentTimeMillis() - tempoInserir);
      System.out.println("| Recuperar...");
      tempoRecuperar = System.currentTimeMillis();
      for(int j = 1; j <= (5550); j++)
        Recuperar(j);
      tempoRecuperar = (System.currentTimeMillis() - tempoRecuperar);
      System.out.println("\n|----[Dados Simulacao Unica]");
        System.out.println("|\n| Tempo para inserir: " + ((tempoInserir/1000.00)) + "s");
        System.out.println("| Tempo para recuperar: " + ((tempoRecuperar/1000.00)) + "s");
        System.out.println("| Tempo Total: " + (((tempoInserir + tempoRecuperar)/1000.00)) + "s");
        System.out.println("| Tamanho das anotacoes: " + tamanhoAnotacao + " caracteres");
        System.out.println("| Tamanho do Arquivo Mestre: " + crudSimulacao.length() + " Bytes");
      System.out.println("| Quantidade de prontuario: " + (5550));
      // Segunda Simulacao
      tamanhoAnotacao = 10;
      for(int i = 0; i < 11; i++){
        criarArquivos(2, 2);
        System.out.println("\n| Inserir...");
        tempoInserir = System.currentTimeMillis();
        for(int j = 1; j <= ((i+1)*200); j++){
          r.CPF = j;
          r.nome = ("Prontuario " + j);
          r.sexo = 'F'; 
          r.dataNascimento = d;
          r.tamanhoAnotacao = tamanhoAnotacao;
          r.anotacoes = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
          r.tamanhoProntuario = r.getTamanhoProntuario();
          if(!inserirNoArquivo(r))  
            System.out.println("\n(!) ERRO: Prontuario ja exite!");
        }
        tempoInserir = (System.currentTimeMillis() - tempoInserir);
        tempoRecuperar = System.currentTimeMillis();
        System.out.println("| Recuperar...");
        for(int j = 1; j <= ((i+1)*200); j++){
          if(!Recuperar(j))
            System.out.println("(!) ERRO: Prontuario(" + (j) + ") nao encontrado");
        }
        tempoRecuperar = (System.currentTimeMillis() - tempoRecuperar);
        System.out.println("\n|----[Dados Simulacao " + (i+1) + "]");
        System.out.println("|\n| Tempo para inserir: " + ((tempoInserir/1000.00)) + "s");
        System.out.println("| Tempo para recuperar: " + ((tempoRecuperar/1000.00)) + "s");
        System.out.println("| Tempo Total: " + (((tempoInserir + tempoRecuperar)/1000.00)) + "s");
        System.out.println("| Tamanho das anotacoes: " + tamanhoAnotacao + " caracteres");
        System.out.println("| Tamanho do Arquivo Mestre: " + crudSimulacao.length() + " Bytes");
        System.out.println("| Quantidade de prontuario: " + (i+1)*200);
        tamanhoAnotacao += 1000;
      }
      // Terceira Simulacao
      tamanhoAnotacao = 10;
      for(int i = 0; i < 11; i++){
        criarArquivos(3, 4);
        System.out.println("\n| Inserir...");
        tempoInserir = System.currentTimeMillis();
        for(int j = 1; j <= ((i+1)*200); j++){
          r.CPF = j;
          r.nome = ("Prontuario " + j);
          r.sexo = 'F'; 
          r.dataNascimento = d;
          r.tamanhoAnotacao = tamanhoAnotacao;
          r.anotacoes = "ABCDEFGH";
          r.tamanhoProntuario = r.getTamanhoProntuario();
          if(!inserirNoArquivo(r))  
            System.out.println("\n(!) ERRO: Prontuario ja exite!");
        }
        tempoInserir = (System.currentTimeMillis() - tempoInserir);
        System.out.println("| Recuperar...");
        tempoRecuperar = System.currentTimeMillis();
        for(int j = 1; j <= ((i+1)*200); j++){
          if(!Recuperar(j))
            System.out.println("(!) ERRO: Prontuario(" + (j) + ") nao encontrado");
        }
        tempoRecuperar = (System.currentTimeMillis() - tempoRecuperar);
        System.out.println("\n|----[Dados Simulacao " + (i+1) + "]");
        System.out.println("|\n| Tempo para inserir: " + ((tempoInserir/1000.00)) + "s");
        System.out.println("| Tempo para recuperar: " + ((tempoRecuperar/1000.00)) + "s");
        System.out.println("| Tempo Total: " + (((tempoInserir + tempoRecuperar)/1000)) + "s");
        System.out.println("| Tamanho das anotacoes: " + tamanhoAnotacao + " caracteres");
        System.out.println("| Tamanho do Arquivo Mestre: " + crudSimulacao.length() + " Bytes");
        System.out.println("| Quantidade de prontuario: " + (i+1)*200);
        tamanhoAnotacao += 1000;
      }
      crudSimulacao.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

//################################################################################################

// CLASS Main - Para fazer as operacaes e representacoes necessarias
class Main {
  /* - FUNCOES CRUD - */
  // Inserçao - Construir os prontuarios e incluir no arquivo mestre, 
  // por meio dos arquivos auxiliares (indice e diretorio)
  public static void insercao(Scanner in, ManipuladorDeArquivos M) {
    // Definiçao
    long tempo;
    boolean result = false;
    Data dataDeNascimento = new Data();
    String linha = "";
    String confirmar = " "; // Confirmador de Inclusao
    Prontuario r = new Prontuario(); // Criar Prontuario
    boolean jaExite = false;
    in.nextLine(); // Limpar entrada
    // Pegar dados
    // Nome
    while (r.nome.length() <= 0) {
      System.out.print("\nColoque o nome (Obs.: Tamanho limite 34 caracteres): ");
      r.nome = in.nextLine();
      if (r.nome.length() <= 0)
        System.out.println("(!) ERRO: O valor passado e invalido");
    }
    // CPF
    while (r.CPF <= 0 || r.CPF > 999999999) {
      System.out.print("\nColoque o CPF: ");
      r.CPF = in.nextInt();
      if (r.CPF <= 0)
        System.out.println("(!) ERRO: O valor passado e invalido");
    }
    in.nextLine(); // Tirar '\n
    // Sexo
    while (r.sexo != 'M' && r.sexo != 'F') {
      System.out.print("\nColoque o sexo: ");
      linha = in.nextLine();
      if (linha.length() != 0)
        r.sexo = linha.charAt(0);
      if (r.sexo != 'M' && r.sexo != 'F')
        System.out.println("(!) ERRO: O valor passado e invalido");
    }
    // Data de nascimento
    System.out.print("\n\nData de Nascimento ");
    while (dataDeNascimento.dia <= 0 || dataDeNascimento.dia > 31) {
      System.out.print("\n\tColoque o dia: ");
      dataDeNascimento.dia = in.nextByte();
      if (dataDeNascimento.dia <= 0 || dataDeNascimento.dia > 31)
        System.out.println("(!) ERRO: O valor passado e invalido");
    }
    while (dataDeNascimento.mes <= 0 || dataDeNascimento.mes > 12) {
      System.out.print("\n\tColoque o mes: ");
      dataDeNascimento.mes = in.nextByte();
      if (dataDeNascimento.mes <= 0 || dataDeNascimento.mes > 12)
        System.out.println("(!) ERRO: O valor passado e invalido");
    }
    System.out.print("\n\tColoque o ano: ");
    dataDeNascimento.ano = (in.nextShort());
    r.dataNascimento = (dataDeNascimento);
    // Tamanho das anotacoes
    System.out.print("\nTamanho limite das anotacoes (em caracteres): ");
    r.tamanhoAnotacao = in.nextInt();
    in.nextLine(); // Tirar '\n'
    // Anotacoes
    System.out.print("\nColoque as anotacoes (Obs.: Tamanho limite de " + r.tamanhoAnotacao + " caracteres): ");
    r.anotacoes = in.nextLine();
    // Mostrar Prontuario
    r.Mostrar();
    //Pedir confirmacao
    System.out.print("\nINCLUIR esse prontuario?[Y/n] ");
    confirmar = in.nextLine();
    if (confirmar.length() == 0 || (confirmar.charAt(0) == 'y' || confirmar.charAt(0) == 'Y')) {
      // Inserir Prontuario
      tempo = System.currentTimeMillis();
      result = M.inserirNoArquivo(r);
      System.out.println("\n Tempo da inserçao: " + ((System.currentTimeMillis() - tempo)) + "ms");
      if(!result)  
        System.out.println("\n(!) ERRO: Prontuario ja exite!");
      else
        System.out.println("\nProntuario foi incluido!");
    } else if (confirmar.charAt(0) == 'n' || confirmar.charAt(0) == 'N') {
      System.out.println("\nProntuario nao foi incluido!");
    } else {
      System.err.println("\nERRO: Procedimento abortado!");
    }
    System.out.print("\n\tPressione Enter para continuar");
    in.nextLine(); // Limpar entrada
  } // fim insercao

  //----------------------------------------------------------------------------------------------

  // Alteracao - Alterar o valor do campo anotação
  public static void alteracao(Scanner in, ManipuladorDeArquivos M) {
    // Definicao
    int cpf = 0;
    in.nextLine(); // Limpar entrada
    // Fazer
    while (cpf < 1 || cpf > 999999999) {
      System.out.print("Coloque o CPF do prontuario: ");
      cpf = in.nextInt();
      if (cpf < 1 && cpf > 999999999)
        System.out.println("(!) ERRO: O valor passado e invalido");
    }
    try {
      M.alterarArquivo(cpf, in);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }// fim alteracao

  //----------------------------------------------------------------------------------------------

  // Exclusao - Excluir de maneira logica um prontuario
  public static void exclusao(Scanner in, ManipuladorDeArquivos M) {
    // Definicao
    String confirmar = " "; // Confirmador de Execlusao
    int cpf = 0;
    in.nextLine(); // Limpar entrada
    while (cpf < 1 || cpf > 999999999) {
      System.out.print("Coloque o CPF do prontuario: ");
      cpf = in.nextInt();
      if (cpf < 1 && cpf > 999999999)
        System.out.println("(!) ERRO: O valor passado e invalido");
    }
    // Pedir confirmacao
    System.out.print("\nEXCLUIR esse prontuario?[Y/n] ");
    confirmar = in.nextLine();
    if (confirmar.length() == 0 || (confirmar.charAt(0) == 'y' || confirmar.charAt(0) == 'Y')) {
      if(M.excluirProntuario(cpf))
        System.out.println("\nProntuario excluido!");
      else
        System.out.println("\nProntuario nao foi excluido!" 
                         + "\n(!) ERRO: Prontuario nao existe!");
    } else if (confirmar.charAt(0) == 'n' || confirmar.charAt(0) == 'N') {
      System.out.println("\nProntuario nao foi excluido!");
    } else {
      System.out.println("\nProcedimento abortado!");
    }
    System.out.print("\n\tPressione Enter para continuar");
    in.nextLine();
  }// fim exclusao

  //----------------------------------------------------------------------------------------------

  // Impressao - Imprimir todos os arquivos que estão sento utilizados
  public static void impressao(Scanner in, ManipuladorDeArquivos M) {
    // Definicao
    M.mostrarArquivos(in);
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
    while (opcao != 0) {
      // Menu
      System.out.print("\n|---[ MENU V-1.1]" +
                       "\n| Opcoes:" +
                       "\n| 0) Sair" +
                       "\n| 1) Criar arquivo" +
                       "\n| 2) Insercao" +
                       "\n| 3) Alteracao" +
                       "\n| 4) Exclusao" +
                       "\n| 5) Impressao" +
                       "\n| 6) Simulacao \n|" +
                       "\n| Escolha: ");
      // Obter escolha
      opcao = leitor.nextInt();
      // Verificar opcao
      switch (opcao) {
        case 0:
          System.out.println("|\n|---[ Encerrado ]");
          break;
        case 1:
          System.out.println("\n|---[ Criar Arquivo ]");
          while (profundidadeGlobalInicial < 1) {
            System.out.print("Coloque a profundidade inicial do diretorio: ");
            profundidadeGlobalInicial = leitor.nextInt();
            if (profundidadeGlobalInicial < 1)
              System.out.println("(!) ERRO: O valor passado e invalido");
          }
          while (quantidadeEntradaBucket < 1) {
            System.out.print("Coloque o maximo de entradas do bucket: ");
            quantidadeEntradaBucket = leitor.nextInt();
            if (quantidadeEntradaBucket < 1)
              System.out.println("(!) ERRO: O valor passado e invalido");
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
          exclusao(leitor, M);
          break;
        case 5:
          System.out.println("\n|---[ Imprimir ]\n|");
          impressao(leitor, M);
          break;
        case 6:
          System.out.println("\n|---[ Simulacao ]");
          M.Simulacao();
          break;
        default:
          System.out.println("\nERRO: Opcao nao encontrada");
      }
    }
    leitor.close();
  } // fim main
} // fim Main