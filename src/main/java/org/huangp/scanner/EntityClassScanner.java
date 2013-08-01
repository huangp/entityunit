package org.huangp.scanner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import org.huangp.internal.BootstrapService;
import org.huangp.internal.NodeProperties;
import org.neo4j.graphdb.Node;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.reflect.Invokable;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Slf4j
public class EntityClassScanner implements ClassScanner
{
   @Override
   public void scan(Class clazz)
   {
      String classUnderScan = clazz.getName();
      log.debug("scanning class: {}", classUnderScan);

      Annotation entityAnnotation = clazz.getAnnotation(Entity.class);
      Preconditions.checkState(entityAnnotation != null, "This scans only entity class");

      Optional<Node> optionalNode = BootstrapService.INSTANCE.tryFindNodeByName(classUnderScan);
      Node startNode;
      if (optionalNode.isPresent())
      {
         log.debug("Entity [{}] already in graph", classUnderScan);
         startNode = optionalNode.get();
         if (startNode.hasProperty(NodeProperties.SCANNED.name()))
         {
            log.debug("{} has been scanned", classUnderScan);
            return;
         }
      }
      else
      {
         startNode = BootstrapService.INSTANCE.addNode(classUnderScan);
      }

      // access type is field
      Annotation access = clazz.getAnnotation(Access.class);
      if (access != null)
      {
         AccessType accessType = ((Access) access).value();
         if (accessType == AccessType.FIELD)
         {
            // field based annotation
            Field[] fields = clazz.getFields();
            for (Field field : fields)
            {
               Annotation[] annotations = field.getAnnotations();
               for (Annotation annotation : annotations)
               {
                  if (annotation.annotationType() == ManyToOne.class)
                  {
                     Class<?> dependingEntityType = field.getType();
                     log.debug("found many to one entity: {} on {}", dependingEntityType.getName(), classUnderScan);

                     Optional<Node> dependingNodeOptional = BootstrapService.INSTANCE.tryFindNodeByName(dependingEntityType.getName());
                     if (!dependingNodeOptional.isPresent())
                     {
                        Node dependingNode = BootstrapService.INSTANCE.addNode(dependingEntityType.getName());
                        BootstrapService.INSTANCE.setDependency(startNode, dependingNode);
                        scan(dependingEntityType);
                     }
                     else
                     {
                        BootstrapService.INSTANCE.setDependency(startNode, dependingNodeOptional.get());
                     }
                  }
               }
            }
         }
      }
      else
      {
         // property based annotation
         Method[] methods = clazz.getMethods();
         for (Method method : methods)
         {
            Invokable<?, Object> invokable = Invokable.from(method);
            if (invokable.isAnnotationPresent(ManyToOne.class))
            {
               Class<?> dependingEntityType = invokable.getReturnType().getRawType();
               log.debug("found many to one entity: {} on {}", dependingEntityType.getName(), classUnderScan);

               Optional<Node> dependingNodeOptional = BootstrapService.INSTANCE.tryFindNodeByName(dependingEntityType.getName());
               if (!dependingNodeOptional.isPresent())
               {
                  Node dependingNode = BootstrapService.INSTANCE.addNode(dependingEntityType.getName());
                  BootstrapService.INSTANCE.setDependency(startNode, dependingNode);
                  scan(dependingEntityType);
               }
               else
               {
                  BootstrapService.INSTANCE.setDependency(startNode, dependingNodeOptional.get());
               }
            }
         }
      }
      BootstrapService.INSTANCE.markNodeScanned(startNode);
   }
}
