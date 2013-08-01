package org.huangp.internal;

import java.io.File;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;
import org.neo4j.server.WrappingNeoServerBootstrapper;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Slf4j
public enum BootstrapService
{
   INSTANCE;
   private GraphDatabaseService graphDb;

   private BootstrapService()
   {
//      String dbPath = System.getProperty("db.path", new File(System.getProperty("java.io.tmpdir"), "data-gen/db").getAbsolutePath());
      // TODO fix this
//      System.out.println("db path is: " + dbPath);
      graphDb = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder("/NotBackedUp/tools/neo4j-server/data/graph.db")
            .setConfig(GraphDatabaseSettings.node_auto_indexing, "true")
            .setConfig(GraphDatabaseSettings.dump_configuration, "true")
            .setConfig(GraphDatabaseSettings.node_keys_indexable, NodeProperties.NAME.name())
            .newGraphDatabase();
//      WrappingNeoServerBootstrapper server = new WrappingNeoServerBootstrapper((GraphDatabaseAPI) graphDb);
//      server.start();

      registerShutdownHook(graphDb);
   }

   public <T> T doInTransaction(Function<GraphDatabaseService, T> function)
   {
      Transaction transaction = graphDb.beginTx();
      try
      {
         T result = function.apply(graphDb);
         transaction.success();
         return result;
      }
      catch (Exception e)
      {
         transaction.failure();
         throw new RuntimeException(e);
      }
      finally
      {
         transaction.finish();
      }
   }

   private static void registerShutdownHook(final GraphDatabaseService graphDb)
   {
      Runtime.getRuntime().addShutdownHook(new Thread()
      {
         @Override
         public void run()
         {
            graphDb.shutdown();
         }
      } );
   }

   public Node addNode(final String name)
   {
      log.debug("adding node with name: {}", name);
      return doInTransaction(new Function<GraphDatabaseService, Node>()
      {
         @Override
         public Node apply(GraphDatabaseService input)
         {
            Node node = input.createNode();
            node.addLabel(EntityLabel.ENTITY);
            node.setProperty(NodeProperties.NAME.name(), name);
            return node;
         }
      });
   }

   private Traverser getEntityDependencies(final String name)
   {
      Optional<Node> startNode = tryFindNodeByName(name);
      Preconditions.checkState(startNode.isPresent(), "Can't find node by name:" + name);
      TraversalDescription td = Traversal.description()
            .depthFirst()
            .uniqueness(Uniqueness.NODE_GLOBAL)
            .relationships(ReferenceRel.DEPENDS_ON, Direction.OUTGOING)
            .evaluator(Evaluators.excludeStartPosition());
      return td.traverse(startNode.get());
   }

   public Optional<Node> tryFindNodeByName(final String name)
   {
      return doInTransaction(new Function<GraphDatabaseService, Optional<Node>>()
      {
         @Override
         public Optional<Node> apply(GraphDatabaseService input)
         {
            ResourceIterable<Node> iter = input.findNodesByLabelAndProperty(EntityLabel.ENTITY, NodeProperties.NAME.name(), name);
            if (Iterables.size(iter) > 0)
            {
               return Optional.of(iter.iterator().next());
            }
            return Optional.absent();
         }
      });
   }

   public String print(String name)
   {
      StringBuilder builder = new StringBuilder(name).append("\n");
      Traverser traverser = getEntityDependencies(name);
      for (Path path : traverser)
      {
         builder.append(" depends on ").append(path.endNode().getProperty(NodeProperties.NAME.name()))
               .append(" on depth ").append(path.length()).append("\n");
      }
      return builder.toString();
   }

   public void setDependency(final Node startNode, final Node dependingNode)
   {
      doInTransaction(new Function<GraphDatabaseService, Void>()
      {
         @Override
         public Void apply(GraphDatabaseService input)
         {
            startNode.createRelationshipTo(dependingNode, ReferenceRel.DEPENDS_ON);
            log.debug("{} depends on {}", startNode.getProperty(NodeProperties.NAME.name()), dependingNode.getProperty(NodeProperties.NAME.name()));
            return null;
         }
      });
   }

   public void markNodeScanned(final Node node)
   {
      doInTransaction(new Function<GraphDatabaseService, Node>()
      {
         @Override
         public Node apply(GraphDatabaseService input)
         {
            node.setProperty(NodeProperties.SCANNED.name(), true);
            return node;
         }
      });
   }
}
