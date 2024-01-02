#import "template.typ": *

#show: project.with(
  title: "Sistemas Distribuídos - Grupo 1",
  authors: (
    (name: "Daniel Pereira", number: "A100545"),
    (name: "Duarte Ribeiro", number: "A100764"),
    (name: "Francisco Ferreira", number: "A100660"),
    (name: "Rui Lopes", number: "A100643"),
  ),
  date: "26 de novembro de 2023",
)

#heading(numbering:none)[Introdução]
Este relatório tem como objetivo apresentar o trabalho prático desenvolvido para a unidade curricular de Sistemas Distribuídos. O trabalho consiste no desenvolvimento de um sistema distribuído que permite a execução de funções remotamente. Deste modo, iremos apresentar a arquitetura do sistema, a sua implementação e as decisões tomadas pelo grupo durante o desenvolvimento do mesmo.

= Arquitetura

Desde o início da realização do projeto, tivemos a ambição de realizar todos as funcionalidades pedidas. Assim, o sistema foi desenvolvido de forma distribuída, a partir de um servidor que reencaminha funções para vários _workers_ que as executam.

#import "@preview/fletcher:0.2.0": *

#figure(caption: "Visão geral da arquitetura", fletcher.diagram(
  node-stroke: black,
  spacing: (10mm, 5mm),
  node-inset: 30pt,
{

  let read_write_thread_circle(pos) = {
      edge(pos, pos, text(size: 7pt)[Thread Leitura], "->", bend: -110deg)
      edge(pos, pos, text(size: 7pt)[Thread Escrita], "->", bend: 110deg)
  }
  
  let cliente1_pos = (0, 1.5)
  node(cliente1_pos, [Cliente A])
  edge(cliente1_pos, (1,0), "<->")
  
  node((0, 0), [Cliente B])
  edge((0, 0), (1,0), "<->")

  node((0, -1), [Cliente C])
  edge((0, -1), (1,0), "<->")

  read_write_thread_circle(cliente1_pos)

  let worker1_pos = (2, 0.9)
  
  node(worker1_pos, [Worker X])
  edge(worker1_pos, (1,0), "<->")
  
  read_write_thread_circle(worker1_pos)
  
  node((2, -0.5), [Worker Y])
  edge((2, -0.5), (1,0), "<->")

  let servidor_pos = (1, 0)

  node((1,0), [Servidor])

  edge(servidor_pos, servidor_pos, text(size: 7pt)[N#footnote[Tantas _threads_ de leitura e escrita quanto o número de clientes e _workers_ conectados.]<NThreads> Threads de Leitura], "=>", bend: -110deg)
  edge(servidor_pos, servidor_pos, text(size: 7pt)[N@NThreads Threads de Escrita], "=>", bend: 110deg)
  
  let pos2 = (1, 0)

}))

Para organização e separação de responsabilidades, o projeto foi desenvolvido com vários módulos#footnote[A gestão (compilação e dependências) destes módulos é feita recorrendo ao #link("https://gradle.org/")[Gradle].]:

- Módulo `client-api`: implementação de lógica do cliente;
- Módulo `client`: interface do cliente sobre o módulo `client-api`;
- Módulo `server`: implementação do programa do servidor;
- Módulo `worker`: implementação do programa do worker;
- Módulo `commons`: módulo de utilitários ou classes comuns aos vários módulos.
\
O módulo `commons` tem presente a implementação de estruturas de dados _thread-safe_, tais como _Map_, _List_ e _Bounded Buffer_, estruturas estas utilizadas extensivamente ao longo do projeto. Além disso, este módulo tem presente pacotes comuns entre as várias conexões (cliente $<->$ servidor e servidor $<->$ worker), bem como o protocolo de serialização explicado #link(<serde>)[mais abaixo].

== Uma Conexão <Conexão>

#let abstract_connection_link = "https://github.com/chicoferreira/sd-cloud-computing/blob/master/common/src/main/java/sd/cloudcomputing/common/AbstractConnection.java"

#let abstract_connection_read_write_link = "https://github.com/chicoferreira/sd-cloud-computing/blob/master/common/src/main/java/sd/cloudcomputing/common/AbstractConnection.java#L53"

#let abstract_connection_handle_packet_link = "https://github.com/chicoferreira/sd-cloud-computing/blob/master/common/src/main/java/sd/cloudcomputing/common/AbstractConnection.java#L90"

#let abstract_connection_enqueue_packet_link = "https://github.com/chicoferreira/sd-cloud-computing/blob/master/common/src/main/java/sd/cloudcomputing/common/AbstractConnection.java#L82"

No módulo `commons` está presente uma classe #link(abstract_connection_link)[```java AbstractConnection<W, R>```] que contém lógica de leitura (de pacotes do tipo $R$) e escrita (de pacotes do tipo $W$) para uma conexão (_socket_). Ao chamar o método #link(abstract_connection_read_write_link)[```java startReadWrite()```], uma _thread_ de escrita e outra de leitura são iniciadas. 

A _thread_ de escrita está a ler de um _Bounded Buffer_ (que espera até ser colocado lá algum pacote). Após ser colocado lá um pacote, através do método #link(abstract_connection_enqueue_packet_link)[```java enqueuePacket(W packet)```], essa _thread_ consome-o, serializa-o para a conexão e faz _flush_. O protocolo TCP é encarregue de fazer com que o pacote chegue garantidamente e corretamente ao destinatário.

A _thread_ de leitura fica à espera de receber um pacote no _socket_. Uma vez que o tamanho e tipo de dados recebidos são previsíveis, o processo de desserialização começa mal é recebido um dado e termina quando é lido o conteúdo esperado. Após a leitura completa do pacote, é chamado o método abstrato #link(abstract_connection_handle_packet_link)[```java handlePacket(R packet)```] na mesma _thread_, que irá correr a lógica esperada para o _handling_ daquele pacote. Cabe à implementação desse método fazer o mínimo possível ou passar o trabalho para outra _thread_.

Vale ressaltar que todas as conexões do programa (cliente $->$ servidor, servidor $->$ cliente, _worker_ $->$ servidor e servidor $->$ _worker_) usam este padrão de duas _threads_. 

= Worker
Ao optar pela implementação distribuída, o programa do _worker_ teve que ser desenvolvido de forma a executar _jobs_ pedidos pelo servidor. Vários destes _workers_ estarão em execução em simultâneo.

Inicialmente, um _worker_ inicia ligando-se ao servidor na porta aberta para conexões de _workers_. Caso o servidor não esteja disponível, o _worker_ fecha não fazendo tentativas de reconexão. O mesmo acontece quando o servidor fecha depois do _worker_ ter feito essa conexão. Não achamos importante uma funcionalidade de reconexão, já que é esperado que o _worker_ tenha sempre o mesmo ou menor tempo de vida que o servidor.

O _worker_ inicia com parâmetros dois parâmetros, passados através da linha de comandos: *#smallcaps[max concurrent jobs]* e *#smallcaps[memory capacity]*. Estes parâmetros indicam, respetivamente, o *número máximo de _jobs_ que podem _rodar_ ao mesmo tempo*, que se traduz no número de _threads_ disponíveis para a execução de _jobs_, e a capacidade de memória, como especificada no enunciado deste trabalho prático.

Ao ligar-se com o servidor, o _worker_ #link(<WSHandshakePacket>)[informa-o da sua capacidade de memória] e a partir daí começa a ler pedidos de _jobs_. Novamente, esta conexão segue o padrão #link(<Conexão>)[especificado anteriormente].

Quando o _worker_ recebe um pedido de _job_ do servidor, insere-o num _Bounded Buffer_. Este, está a ser consumido por *#smallcaps[max concurrent jobs]* _threads_. Só *uma* _thread_ pode receber *um* valor consumido.

Quando uma _thread_ livre consome um pedido, verifica se a utilização de memória atual com a soma da memória necessária do _job_ é menor do que a capacidade de memória do _worker_, caso contrário, espera até que seja maior, e então chama o _JobFunction_ para os _bytes_ recebidos. Se a memória necessária para executar o _job_ for maior do que a capacidade total de memória do _worker_, ele é simplesmente ignorado. Cabe ao servidor de não enviar _jobs_ que o _worker_ não consiga executar.

No fim da execução, em caso de sucesso ou erro, constrói a resposta adequada e coloca-a para ser escrita na conexão para o servidor #footnote[Entende-se com isto adicionar a resposta no _Bounded Buffer_ da _thread_ de escrita da conexão.].

O _worker_ executa, portanto, os _jobs_ recebidos pelo servidor por ordem de chegada, sem fazer reordenação e concorrentemente sempre que tiver memória disponível para tal#footnote[Também existe o caso limite de existirem sempre _threads_ livres, mas assume-se que a pessoa que configura o _worker_ escolhe um número adequado de #smallcaps[max concurrent jobs] para o número de #smallcaps[memory capacity] também fornecido.]. Cabe ao servidor de fazer o escalonamento correto. O algoritmo de escalonamento usado pelo servidor será explicado num #link(<Algoritmo>)[capítulo posterior].

= Cliente

O programa do cliente foi feito baseado numa _CLI (command-line interface)_ que, enquanto está a ser executado, lê comandos a partir do _input_ do utilizador e executa-os. 

```sh
[INFO] Type 'help' to see all available commands.
> help
[INFO] Available commands:
[INFO]  - benchmark <numberXmemory>...
[INFO]  - connect <host> <port>
[INFO]  - disconnect 
[INFO]  - exit 
[INFO]  - help 
[INFO]  - job <file> <memory> (output path)
[INFO]  - jobs 
[INFO]  - status 
```

Como a interface terá mensagens a serem escritas enquanto o utilizador está a escrever no input (e.g. receber um resultado de um _job_), foi usada a biblioteca _JLine_ #footnote("https://github.com/jline/jline3") para resolver o problema do _standard output_ escrever à frente do input do utilizador.

== Comando

A interface do utilizador é baseada em comandos no terminal (dentro da aplicação), onde cada um executa uma determinada tarefa. Cada comando tem também um nome (a forma como são chamados) e um guia de utilização.  Os comandos seguem o formato ```sh comando argumentos...```, onde cada argumento pode ser obrigatório `<arg>` ou opcional `(arg)`.

== Autenticação

O utilizador do programa pode-se conectar ao servidor através do comando ```sh connect <host> <port>```. A maioria dos comandos não podem ser executados até que o cliente se autentique com o servidor.

```sh
> status
[ERROR] Not connected to server
```

Após o programa abrir um _socket_ para o servidor, pede ao utilizador o _username_ e _password_, com o fim de se autenticar no servidor. 

```sh
> connect localhost 8080
[INFO] Connected to server at 127.0.0.1:8080
Insert your username: meu_utilizador
Insert your password: ****
[INFO] Successfully authenticated as meu_utilizador (REGISTERED)
```

Após escrita dos campos, esses são serializados #link(<CSAuthPacket>)[no pacote de autenticação] e enviados para o servidor. O servidor responde então com o resultado da autenticação. Caso o resultado tenha sido positivo, as _threads_ de escrita e leitura da conexão são iniciadas, novamente, com o padrão de conexão #link(<Conexão>)[especificado anteriormente]. A partir desse momento, o utilizador tem acesso aos comandos relativos a _jobs_ e _status_ do servidor. Em caso contrário, a conexão com o servidor é fechada.

== Pedidos ao servidor

A partir do momento que o utilizador se autentica no servidor, já lhe pode enviar pacotes.
```sh 
> job job.txt 50
[INFO] Sent job with id 0 with 75 bytes of data
```

```sh 
> status
[INFO] Sent server status request. Response will be printed when received...
[INFO] Server status (127.0.0.1:8080): 
[INFO]  - Connected workers: 3
[INFO]  - Total capacity: 250MB
[INFO]  - Max possible memory: 100MB
[INFO]  - Memory usage: 0%
[INFO]  - Jobs running: 0
```

#let client_job_link = "https://github.com/chicoferreira/sd-cloud-computing/blob/master/client-api/src/main/java/sd/cloudcomputing/client/api/ClientJob.java"

#let job_manager_linK = "https://github.com/chicoferreira/sd-cloud-computing/blob/master/client-api/src/main/java/sd/cloudcomputing/client/api/ClientJobManager.java"

O comando `job <ficheiro> <memória> (output)` envia #link(<JobRequest>)[um pedido de execução de _job_ ao servidor]. Quando executado, este lê os _bytes_ do ficheiro de _input_, gera um _id_ como forma de identificar a que _job_ uma resposta futura se refere e coloca ambos os dados, juntamente com a memória necessária, num pacote. Assume-se que o ficheiro carregado não é grande demais para não caber numa região contígua de memória. Além disso, são também guardadas informações do ```java System.nanoTime()``` atual para cálculo de _delay_ de resposta e o nome do ficheiro de _output_ para futura escrita em caso de sucesso (#link(client_job_link)[```java ClientJob```] e #link(job_manager_linK)[```java ClientJobManager```]).

O comando `status` envia #link(<CSServerStatusRequestPacket>)[um pedido de estado ao servidor]. Nada é colocado junto ao pacote. O estado recebido tem as informações globais do servidor (o mesmo para todos os clientes). Ao receber a resposta deste pedido, como o servidor envia sempre o estado mais atualizado, não precisámos de saber a que pedido a resposta se refere, já que podemos assumir que o pacote mais recente #footnote[O TCP é responsável por assegurar a ordem correta de entrega dos pacotes.] é o que tem informações mais atualizadas.

Ambos os comandos bloqueiam a _thread_ de input *apenas* até o pacote ser colocado no _Bounded Buffer_ de escrita da conexão. A resposta é impressa no terminal quando o cliente a recebe. Isto permite ao cliente poder submeter novos pedidos enquanto não recebe resposta dos anteriores.

Também existe um comando `benchmark` que permite enviar vários pedidos de _jobs_ (sem conteúdo) facilmente. Este comando foi usado extensivamente como auxílio no teste e validação do algoritmo de escalonamento. 

```sh
> benchmark 50x50 40x20 30x20
[INFO] Sending 50 jobs with 50MB memory
[INFO] Sending 40 jobs with 20MB memory
[INFO] Sending 30 jobs with 20MB memory
```

== Resultado do _job_ e escrita do resultado em ficheiro

O #link(<JobResult>)[resultado de um _job_ recebido por um cliente] pode ter vários tipos:

- *Sucesso*, com os _bytes_ de resultado
- *Falha*, com um código e uma mensagem de erro
- *Sem memória*, sem informação adicional

```sh
> job job.txt 50
[INFO] Sent job with id 0 with 5 bytes of data
[INFO] Job 0 completed successfully with 25 bytes.
[INFO] Saved job result for job 0 to file job-0.7z
> job job.txt 50
[INFO] Sent job with id 1 with 5 bytes of data
[ERROR] Job 1 failed with error code 138: Could not compute the job.
> job job.txt 150
[INFO] Sent job with id 2 with 5 bytes of data
[INFO] Job 2 failed due to not enough memory
```

Para qualquer tipo, é mostrada uma mensagem no terminal sobre informações do mesmo.

#let job_result_file_worker = "https://github.com/chicoferreira/sd-cloud-computing/blob/master/client-api/src/main/java/sd/cloudcomputing/client/api/JobResultFileWorker.java"

Em caso de sucesso, o resultado (juntamente com o nome do ficheiro) é _queued_ para escrita em ficheiro no #link(job_result_file_worker)[```java JobResultFileWorker```]. Esta classe é responsável por iniciar e manter uma _thread_ a ler de um _Bounded Buffer_, que, ao ser colocado lá um resultado de sucesso de um _job_, escreve-o para o ficheiro pretendido.


== Listagem de _jobs_ pendentes e concluídos

Foi também desenvolvido um comando para listar todos os _jobs_ pendentes e recebidos:
```sh
> jobs
[INFO] 
[INFO] JOBS (1 scheduled, 123 finished, 124 total)
[INFO] 
[INFO]  Scheduled jobs (1):
[INFO]   123  job-123.7z     50MB     2s ago
[INFO] 
[INFO]  Received jobs (123):
[INFO]   105  job-105.7z     20MB     7s ago  SUCCESS
[INFO]    88   job-88.7z     20MB     7s ago  SUCCESS
[INFO]    59   job-59.7z     20MB     8s ago  FAILURE
[INFO]  116 more...
[INFO] 
```

Este comando usa informações locais ao cliente e não faz nenhum pedido ao servidor.

== API

Todas as funcionalidades da interface do cliente foram construídas usando o módulo `client-api`.

Uma demonstração da sua utilização pode ser vista aqui:

```java
Client client = Client.createNewClient();
ServerNoAuthSession noAuthSession = client.connect("localhost", 8080);
AuthenticateResult authResult = noAuthSession.login("meu_username", "minha_pass");
if (!authResult.isSuccess()) return;

ServerSession session = 
    noAuthSession.createLoggedSession(logger, client, my_listener);
session.startReadWrite();

session.scheduleJob(1, new byte[1024], 50); // Send job request
session.sendServerStatusRequest(); // Send server status request
```

== Limitações e problemas do cliente


Durante o desenvolvimento do trabalho notámos algumas limitações e problemas no cliente que decidimos não resolver no trabalho prático, apresentando, portanto, justificações para tal.

Um dos problemas é que o nosso ```java Logger``` não tem qualquer tipo de controlo de concorrência para mensagens de várias linhas. Na execução de comandos como o `jobs` ou o `status` as chamadas consecutivas ao método ```java logger.info()``` para apresentar o resultado do comando, podem-se intercalar, por exemplo, com mensagens recebidas de resultados de _jobs_.

```sh
[INFO] 
[INFO] JOBS (1 scheduled, 123 finished, 124 total)
[INFO] Job 123 completed successfully with 25 bytes.   # <-------
[INFO] 
[INFO] Saved job result for job 123 to file job-123.7z # <-------
[INFO]  Scheduled jobs (1):
[INFO]   123  job-123.7z     50MB        2s ago
[INFO] 
```

Para manter a simplicidade de uso do _logger_, e por se tratar de um problema meramente visual, não foi implementado um _fix_ para isto.

Outra limitação que notámos foi o facto de o cliente não ter qualquer forma de esperar por mensagens bloqueando a _thread_ a meio da execução #footnote[Apesar de isto acontecer, por exemplo, quando o cliente espera pelo resultado de autenticação, não acontece nunca mais após o ```java startReadWrite()``` ser chamado.]. Isto poderá ser necessário numa futura expansão do projeto que necessite de bloquear o cliente até obter resposta do servidor. Pelo mesmo motivo, de manter a simplicidade da API de _handle_ dos pacotes recebidos, e, também, por falta de necessidade, esta funcionalidade não foi desenvolvida.

= Servidor

O servidor é responsável por fazer a ligação, indiretamente, entre o cliente e os _workers_. Ele inicia abrindo dois _ServerSockets_, um para receber conexões de clientes e outro para receber conexões de _workers_. 

Assumimos que a única porta exposta para o exterior é a porta do _ServerSocket_ dedicado a conexões de clientes, pelo que, não existe nenhuma validação de autenticidade dos _workers_.

Quando o servidor recebe alguma ligação em qualquer das duas portas, o processo de autenticação (clientes) ou _handshake_ (_workers_) começa. Esse processo acontece numa _thread_ separada para evitar possíveis clientes ou _workers_ lentos. 

== Autenticação/_Handshake_

Quando um cliente se conecta, o servidor espera que receba os parâmetros de autenticação do utilizador. Quando recebe, verifica se já existe um utilizador com aquele nome. Se sim, verifica se a password recebida corresponde à que tem guardada, se não, regista o utilizador com essa password. Em caso de sucesso, iniciam-se as _threads_ de escrita e leitura da conexão, novamente com o padrão de conexão #link(<Conexão>)[especificado anteriormente]. Em caso de insucesso, o _socket_ para o cliente é fechado. Em qualquer um dos casos, o servidor envia o #link(<SCAuthResult>)[resultado de autenticação] (registado, logado ou password errada) para o cliente.

Por este programa ser desenvolvido apenas para fins educacionais, as passwords estão guardadas em memória sem qualquer tipo de _hashing_. Sendo também enviadas através da rede sem qualquer tipo de encriptação.

Os _workers_ seguem um processo similar, mas não têm qualquer autenticação. Quando um _worker se conecta_, o servidor espera (também noutra _thread_) por um _handshake_. #link(<WSHandshakePacket>)[Este pacote tem as informações de capacidade de memória máxima do _worker_]. Após recebê-lo, o processo é o mesmo, começando as _threads_ de leitura e escrita.

== Redirecionamento de _jobs_

Como os identificadores (IDs) dos _jobs_ dos clientes não são únicos (vários clientes podem enviar um _job_ com o mesmo identificador), esse identificador é mapeado para um identificador interno único ao servidor, antes do _job_ ser escalonado para os _workers_. Isto também traz o benefício de anonimato entre clientes e _workers_. Quando o servidor recebe a resposta de um _worker_, o identificador é mapeado de volta para o identificador original que o cliente enviou. Neste mapeamento também é guardado o cliente que enviou o _job_, para identificação posterior quando o servidor receber o resultado do _worker_.

== Algoritmo de escalonamento <Algoritmo>

A funcionalidade do escalonamento para os _workers_ foi a parte que teve mais discussão e planeamento deste projeto. No planeamento partiu-se dos seguintes pressupostos:

- Assume-se que os workers não são lentos e têm o mesmo _hardware_ (o tempo médio de execução de um _job_ é igual em todos);
- Um _worker_ executa os _jobs_ por ordem de chegada, sem reordenamento e assim que tenha memória;
- O servidor consegue saber a memória disponível atual de um _worker_ subtraindo a capacidade total de memória do _worker_ com a soma de todas as memórias dos _jobs_ pendentes. 
\
#let schedule_job_request_link = "https://github.com/chicoferreira/sd-cloud-computing/blob/master/server/src/main/java/sd/cloudcomputing/server/OvertakingJobSchedulerImpl.java#L27"
Quando o servidor recebe um _job_ para ser escalonado, invoca a função #link(schedule_job_request_link)[`scheduleJob`].

O funcionamento dessa função define o que o servidor deve fazer quando recebe um _job_ para escalonar. O algoritmo segue a seguinte lógica:

#box(stroke: gray, radius: 10pt, inset: 10pt)[
+ Ao receber um _job_ tenta encontrar um _worker_ com memória disponível atual para o _job_;
+ Caso não encontre:
  + Se o _job_ não foi anteriormente colocado como pendente:
    + Coloca-o numa lista de pendentes com o valor de ultrapassagens a 0;
    + Termina.
  + Se não e se o número de ultrapassagens que o _job_ sofreu for maior que um valor estipulado:
      + Encontra o _worker_ com mais memória livre atualmente que consiga executar o _job_#footnote[Um _worker_ consegue executar um _job_ caso tenha capacidade de memória maior ou igual do que o _job_ requer.] futuramente;
+ Reencaminha o pedido para o _worker_ escolhido (em 1. ou em 2.2.1.);
+ Adiciona 1 ao número de ultrapassagens de todos os _jobs_ que foram escalonados antes deste.
]

Esta função também é executada sempre que o estado de memória de _workers_ atualiza, ou seja:
- Quando um _worker_ termina um _job_.
- Quando um _worker_ se desconecta.

\
Isto permite ao servidor escalonar o máximo de _jobs_ disponíveis, com ultrapassagens possíveis, e sem que pedidos fiquem para trás por um tempo indeterminado. O número máximo de ultrapassagens é um valor mágico, mas no nosso trabalho definimos como 50, isto é, caso nunca encontre um _worker_ com memória disponível para tal, só podem ultrapassar 50 outros _jobs_ para que este seja reencaminhado para um _worker_ mesmo que esse não tenha memória disponível atualmente.

=== Algumas considerações adicionais

O passo 2.2.1 do algoritmo assume que o _worker_ com mais memória livre será o _worker_ que mais rapidamente terá memória disponível para o executar. Isto apesar de ser uma aproximação boa, não é sempre ideal, visto que outro _worker_ mais cheio pode estar a completar um _job_ com muita utilização de memória, e então, ao completá-la poderá ficar, mais rapidamente, esse livre. Uma escolha de _worker_ que permita prever o futuro com mais precisão melhorará o algoritmo de escalonamento atual.

Também existe o problema de quando um _worker_ se desconecta, alguns _jobs_ podem ter mais memória necessária do que os _workers_ restantes têm de capacidade, o que os torna impossíveis de serem executados. Nesse caso, o passo 2.2.1. do algoritmo não encontrará _workers_ pelo que enviará "sem memória" como resultado para o _job_ de volta para o cliente.

Para além disso, quando um _worker_ se desconecta, todos os _jobs_ previamente escalonados para ele que ainda estão por terminar, são novamente escalonados.

== Resultado de um _job_

Quando um _worker_ envia um resultado de um _job_ de volta para o servidor, ou o algoritmo de escalonamento definiu que não existem _workers_ com capacidade de memória para executá-lo, o mapeamento do identificador interno é trocado para o identificador enviado pelo cliente (como dito anteriormente) e a #link(<JobResult>)[resposta (sendo ela sucesso, insucesso ou sem memória)] é colocada para escrita na _thread_ de escrita para o cliente certo#footnote[Como dito anteriormente, o mapeamento também guarda o cliente que enviou o _job_. Portanto, é possível identificá-lo por aí.]. Caso o cliente tenha se desconectado durante a execução do _job_, o resultado é simplesmente ignorado. Com isto, o ciclo de vida de um _job_ acaba.

== Estado do servidor

Um cliente pode pedir pelo estado atual do servidor. Quando o cliente o pede, o servidor reúne essas informações de todos os _workers_, faz os cálculos necessários para percentagens e afins, empacota-os no #link(<SCServerStatusResponsePacket>)[pacote de resposta], e coloca-o na _thread_ de escrita do cliente. Por ser um processo leve, não é criada nenhuma outra _thread_, e então estes cálculos são feitos na _thread_ de leitura referente ao cliente que enviou o pedido.

= Protocolo (Serialização e Desserialização) <serde>

De forma a manter uma boa extensibilidade do projeto, uma biblioteca simples para serialização e desserialização de pacotes foi desenvolvida. Esta biblioteca, apelidada de *Frost*#footnote[Em referência à biblioteca #link("https://github.com/EsotericSoftware/kryo")[Kryo], mas com um significado menos forte.], contém as classes necessárias para a serialização e desserialização de pacotes, bem como interfaces para a implementação de pacotes serializáveis.

== API

#let frost_link = "https://github.com/chicoferreira/sd-cloud-computing/blob/master/common/src/main/java/sd/cloudcomputing/common/serialization/Frost.java"

#let serialize_interface_link = "https://github.com/chicoferreira/sd-cloud-computing/blob/master/common/src/main/java/sd/cloudcomputing/common/serialization/Serialize.java"

Um pacote `T` é serializável se houver uma interface #link(serialize_interface_link)[```java Serialize<T>```] implementada e registada na instância de #link(frost_link)[```java Frost```].

```java
public interface Serialize<T> {
    @NotNull T deserialize(SerializeInput input, Frost frost);
    void serialize(T object, SerializeOutput output, Frost frost);
}
```

Um exemplo de implementação desta classe pode ser vista aqui:

```java
public record CSAuthPacket(String username, String password) {
    public static class Serialization implements Serialize<CSAuthPacket> {
        @Override
        public CSAuthPacket deserialize(SerializeInput input, ...) {
            String username = frost.readString(input);
            String password = frost.readString(input);
            return new CSAuthPacket(username, password);
        }
        @Override
        public void serialize(CSAuthPacket object, SerializeOutput output, ...) {
            frost.writeString(object.username(), output);
            frost.writeString(object.password(), output);
        }}}
```

Dentro da classe #link(frost_link)[```java Frost```], podem ser encontrados mais métodos utilitários para escrita de primitivas e classes serializáveis.

#block(breakable: false)[
Com esta classe implementada, utilizações da mesma podem ser feitas da seguinte forma:

```java
Frost frost = new Frost();
frost.registerSerializer(CSAuthPacket.class, new CSAuthPacket.Serialization());

frost.writeSerializable(new CSAuthPacket(...), CSAuthPacket.class, output);
frost.flush(output);
CSAuthPacket read = frost.readSerializable(CSAuthPacket.class, input);
```
]

Mais exemplos podem ser encontrados em #link("https://github.com/chicoferreira/sd-cloud-computing/tree/master/common/src/test/java/sd/cloudcomputing/common")[testes unitários] ou no resto do programa.

== Pacote Genérico

#let generic_packet = "https://github.com/chicoferreira/sd-cloud-computing/blob/master/common/src/main/java/sd/cloudcomputing/common/protocol/GenericPacket.java"

#let generic_packet_serializer = "https://github.com/chicoferreira/sd-cloud-computing/blob/master/common/src/main/java/sd/cloudcomputing/common/protocol/GenericPacketSerializer.java"

Como visto nos exemplos de API anteriores, o *Frost* já sabe à partida qual é o tipo de pacote para o ler corretamente da _stream_ de _bytes_ recebidos dos _sockets_. Contudo, não conseguimos saber à _priori_, por exemplo, qual é o tipo de pacote que um servidor recebe ao fazer leitura de pacotes de clientes#footnote[Pode ser um pacote de pedido de _job_ ou um pacote de pedido de estado global do servidor.]. Para resolver este problema, existe um pacote #link(generic_packet)[```java GenericPacket<T>```] (que também tem #link(generic_packet_serializer)[uma classe a implementar a interface] ```java Serialize<T>```) que adiciona um campo de identificador de pacote (inteiro de 4 _bytes_) antes do conteúdo do pacote. Com este identificador, único para cada tipo de pacote, conseguimos identificar qual é e ler o conteúdo do pacote de acordo com o mesmo.

== Descrição protocolar de cada pacote

As mensagens entre todas as entidades do programa são enviadas e lidas em formato binário _big-endian_. Faremos a descrição protolocar de cada pacote, indicando cada campo, o tipo de dados e a sua descrição. O tipo de dados será sempre um tipo de dados simples e usa o mesmo padrão de serialização/desserialização da classe disponível no Java #link("https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/DataInputStream.html")[DataInputStream]/#link("https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/DataOutputStream.html")[DataOuputStream].

#import "@preview/tablex:0.0.6": tablex, rowspanx, colspanx

#let packet_links = (
  CSAuthPacket: "https://github.com/chicoferreira/sd-cloud-computing/blob/master/common/src/main/java/sd/cloudcomputing/common/protocol/CSAuthPacket.java",
  SCAuthResult: "https://github.com/chicoferreira/sd-cloud-computing/blob/master/common/src/main/java/sd/cloudcomputing/common/protocol/SCAuthResult.java",
  WSHandshakePacket: "https://github.com/chicoferreira/sd-cloud-computing/blob/master/common/src/main/java/sd/cloudcomputing/common/protocol/WSHandshakePacket.java",
  JobRequest: "https://github.com/chicoferreira/sd-cloud-computing/blob/master/common/src/main/java/sd/cloudcomputing/common/JobRequest.java",
  JobResult: "https://github.com/chicoferreira/sd-cloud-computing/blob/master/common/src/main/java/sd/cloudcomputing/common/JobResult.java",
  CSServerStatusRequestPacket: "https://github.com/chicoferreira/sd-cloud-computing/blob/master/common/src/main/java/sd/cloudcomputing/common/protocol/CSServerStatusRequestPacket.java",
  SCServerStatusResponsePacket: "https://github.com/chicoferreira/sd-cloud-computing/blob/master/common/src/main/java/sd/cloudcomputing/common/protocol/SCServerStatusResponsePacket.java"
)

#let packet_title_color(from, to) = {
  if from == "Cliente" and to == "Servidor" {
    blue.lighten(30%)
  } else if from == "Servidor" and to == "Cliente" {
    red.lighten(30%)
  } else if from == "Servidor" and to == "Worker" {
    orange.lighten(30%)
  } else if from == "Worker" and to == "Servidor" {
    green.darken(30%)
  } else {
    black.lighten(50%)
  }
}

#let packet_direction_box(from, to, background) = box(fill: background, radius: 5pt, inset: 5pt, text(fill: white, size: 10pt, font: "Fira Sans", [#from $->$ #to]))

#let packet_title(title, from_to) = [#place(left,
  from_to.map(array => packet_direction_box(array.at(0), array.at(1), packet_title_color(array.at(0), array.at(1))))
  .join([ \ ])) *#link(packet_links.at(title.split(" ").first()), title)*]

#let packet_def(packet_id: none, packet_name, from_to, description, ..c) = block(breakable: false, tablex(
  columns: (25%, 20%, 1fr),
  rows: (10pt + (20pt * from_to.len()), 20pt, 15pt, auto),
  align: center + horizon,
  stroke: 0.5pt + gray.darken(70%),
  fill: (col, row) => {
    if row == 2 {
      gray.lighten(30%)
    } else if col == 0 and row > 1 {
      gray.lighten(50%)
    } else {
      white
    }
  },
  colspanx(fill: gray.lighten(20%), 3)[#packet_title(packet_name, from_to)],
  colspanx(fill: gray.lighten(20%), 3)[#description],
  [Campo], [Tipo de dados], [Descrição],
  ..(if packet_id != none {
    ([Packet Id], [int], [Sempre igual a #packet_id])
  }),
  ..c
))

<CSAuthPacket>
#packet_def("CSAuthPacket", (("Cliente", "Servidor"),),
  "Enviado quando um cliente quer se autenticar.",
  [Username], [String], [Username do cliente a autenticar],
  [Password], [String], [Password do cliente a autenticar]
)

<SCAuthResult>
#packet_def("SCAuthResult", (("Servidor", "Cliente"),),
  "Enviado pelo servidor ao cliente em resposta ao pedido de autenticação.",
  [AuthenticateResult], [int], [0 = Logado com sucesso\ 1 = Password errada \ 2 = Registado com sucesso]
)

<WSHandshakePacket>
#packet_def("WSHandshakePacket", (("Worker", "Servidor"),),
  [Enviado pelo _worker_ ao servidor para o servidor conhecer as suas capacidades.],
  [Max Memory \ Capacity], [int], [Capacidade de memória suportada pelo _worker_]
)

Como estes pacotes são enviados no começo da conexão, e com isto, estes pacotes são esperados, não é necessário prefixar os pacotes com identificadores como os próximos serão.

<JobRequest>
#packet_def("JobRequest", (("Cliente", "Servidor"),("Servidor", "Worker")),
  packet_id: 1,
  [Enviado pelo cliente ao servidor ou pelo servidor aos _workers_ para fazer um pedido de _job_.],
  [Job Id], [int], [Identificador do pacote \ (para ser usado na resposta)],
  [Data], [byte[] #footnote[_Array_ de _bytes_ são serializadas com o seu tamanho em `int` como prefixo e o seu conteúdo de seguida. Na desserialização, esse inteiro é lido e é alocada uma _array_ desse tamanho, colocando nela o conteúdo sem serem necessárias realocações.]<bytearray>], [Bytes do _job_ a ser executado],
  [Memory Needed], [int], [Memória necessária para o _job_]
)

<JobResult>
#packet_def("JobResult (Em caso de sucesso)", (("Servidor", "Cliente"),("Worker", "Servidor")),
  packet_id: 2,
  [Enviado pelo servidor ao cliente ou _worker_ ao servidor para informar do resultado de um _job_.],
  [Result Type], [int], [Sempre igual a 0],
  [Job Id], [int], [Identificador do pacote (usado no pedido)],
  [Data], [byte[] @bytearray], [Bytes resultantes do _job_]
)

#packet_def("JobResult (Em caso de erro)", (("Servidor", "Cliente"),("Worker", "Servidor")),
  packet_id: 2,
  [Enviado pelo servidor ao cliente ou _worker_ ao servidor para informar do resultado de um _job_.],
  [Result Type], [int], [Sempre igual a 1],
  [Job Id], [int], [Identificador do pacote (usado no pedido)],
  [Error Code], [int], [Código de erro da execução do _job_],
  [Error Message], [String], [Mensagem de erro da execução do _job_]
)

#packet_def("JobResult (Em caso de sem memória)", (("Servidor", "Cliente"),),
  packet_id: 2,
  [Enviado pelo servidor ao cliente para informar que não há _workers_ com a memória necessária.],
  [Result Type], [int], [Sempre igual a 2],
)

<CSServerStatusRequestPacket>
#packet_def("CSServerStatusRequestPacket", (("Cliente", "Servidor"),),
  packet_id: 3,
  [Enviado pelo cliente ao servidor no pedido de estado global do servidor.],
)

<SCServerStatusResponsePacket>
#packet_def("SCServerStatusResponsePacket", (("Servidor", "Cliente"),),
  packet_id: 4,
  [Enviado pelo servidor ao cliente como resposta ao pedido de estado global do servidor.],
  [Connected Workers], [int], [Número de _workers_ conectados ao servidor],
  [Total Capacity], [int], [Soma total de memória \ disponível em cada um dos _workers_],
  [Max Possible Mem.], [int], [Memória máxima de todos os _workers_],
  [Memory Usage %], [int], [Percentagem de uso de memória nos _workers_],
  [Jobs Currently \ Running], [int], [Número de _jobs_ a serem executados neste momento]
)

#heading(numbering: none)[Conclusão]

Para concluir, o trabalho prático foi desenvolvido com sucesso, cumprindo todos os requisitos do enunciado. Consideramos que a realização do trabalho foi uma grande oportunidade para aprender e explorar mais sobre programação concorrente e sistemas distribuídos, aplicando conceitos aprendidos dentro e fora das aulas.