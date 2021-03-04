# Property Based Resilience Workshop

Once you checked out this repository, please run `./gradlew :integration:intTest`. This might take quite some time (> 10 minutes) 
as all docker images are pulled and new ones are built. After the initial build, layers containing the jars are in the docker build
cache. As long as you are not adding or removing dependencies, a rebuild should be much less costly.

The repository has three main branches: main, solution and exercises. The main branch points to the same commit as exercises does.
Branch solution is a few commits behind. The first Exercise can be found in class `de.gtrefs.coffeeshop.integration.CounterShould`.
Please follow the description there. You can look into branch solution if you don't know how to proceeed.

I recommend, that you import the project into your IDE. From there, it should be easier to run the examples.
