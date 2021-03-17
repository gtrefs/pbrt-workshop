# Property Based Resilience Workshop

Welcome! You are the proud owner of a new coffee shop. You spend some money on your point-of-sale (POS)
software and your trusted consulting company installed it. However, there seems to be some odd hick-ups
and not all promised features are implemented. As the money runs tight, you already skipped some training
for your Baristas. So it is up to you to fix your software.

## The architecture

The drawing below depicts an abstract overview how the coffee, order and payment service interact
with each other. The order service accepts an order and asks the barista at the coffee service (1.)
to create the corresponding coffee flavor. Once the coffee is ready, the order service asks the
payment service (2.) to charge the credit card given in the order.

<pre>
                     +--------+
              1.     |        |
        +----------->| Coffee |
        |            |        |
+-------+-+          +--------+
|         |
|  Order  |
|         |
+-------+-+          +---------+
        |     2.     |         |
        +----------->| Payment |
                     |         |
                     +---------+
</pre>

From a technical perspective, all services use Spring Boot. While the coffee and payment service use 
 the traditional Spring MVC model, the order service uses Spring Webflux. All services are deployed
 as a container. The corresponding Dockerfiles are in the repository.

## Getting started
Once you checked out this repository, please run `./gradlew :integration:intTest`. This might take quite some time (> 10 minutes) 
as all docker images are pulled and new ones are built. After the initial build, layers containing the jars are in the docker build
cache. As long as you are not adding or removing dependencies, a rebuild should be much less costly. Some tests will fail.

The repository has two main branches: main and solution. Branch solution is a few commits behind having all solutions. 
The first Exercise can be found in class `de.gtrefs.coffeeshop.integration.CounterWithModelShould`.
Please follow the description there. You can look into branch solution if you don't know how to proceed.

I recommend, that you import the project into your IDE. From there, it should be easier to run the examples.