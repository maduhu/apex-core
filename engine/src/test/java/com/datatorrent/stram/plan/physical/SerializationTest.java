/**
 * Copyright (c) 2012-2013 DataTorrent, Inc.
 * All rights reserved.
 */
package com.datatorrent.stram.plan.physical;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datatorrent.api.Context.OperatorContext;
import com.datatorrent.stram.engine.GenericTestOperator;
import com.datatorrent.stram.plan.TestPlanContext;
import com.datatorrent.stram.plan.logical.LogicalPlan;
import com.datatorrent.stram.plan.logical.LogicalPlan.OperatorMeta;
import com.datatorrent.stram.plan.physical.PhysicalPlanTest.PartitioningTestOperator;

public class SerializationTest
{
  private static final Logger LOG = LoggerFactory.getLogger(SerializationTest.class);

  @Test
  public void test1() throws Exception
  {
    LogicalPlan dag = new LogicalPlan();

    GenericTestOperator o1 = dag.addOperator("o1", GenericTestOperator.class);
    PartitioningTestOperator o2 = dag.addOperator("o2", PartitioningTestOperator.class);
    GenericTestOperator o3 = dag.addOperator("o3", GenericTestOperator.class);

    dag.addStream("o1.outport1", o1.outport1, o2.inport1, o2.inportWithCodec);
    dag.addStream("mergeStream", o2.outport1, o3.inport1);

    dag.getMeta(o2).getAttributes().put(OperatorContext.INITIAL_PARTITION_COUNT, 3);
    dag.getAttributes().put(LogicalPlan.CONTAINERS_MAX_COUNT, 2);

    TestPlanContext ctx = new TestPlanContext();
    PhysicalPlan plan = new PhysicalPlan(dag, ctx);

    ByteArrayOutputStream  bos = new ByteArrayOutputStream();
    LogicalPlan.write(dag, bos);
    LOG.debug("logicalPlan size: " + bos.toByteArray().length);

    bos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(bos);
    oos.writeObject(plan);
    LOG.debug("physicalPlan size: " + bos.toByteArray().length);

    ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
    plan = (PhysicalPlan)new ObjectInputStream(bis).readObject();

    dag = plan.getDAG();

    Field f = PhysicalPlan.class.getDeclaredField("ctx");
    f.setAccessible(true);
    f.set(plan, ctx);
    f.setAccessible(false);

    OperatorMeta o2Meta = dag.getOperatorMeta("o2");
    List<PTOperator> o2Partitions = plan.getOperators(o2Meta);
    Assert.assertEquals(3, o2Partitions.size());
    for (PTOperator o : o2Partitions) {
      Assert.assertNotNull("partition null " + o, o.getPartitionKeys());
      Assert.assertEquals("partition keys " + o + " " + o.getPartitionKeys(), 2, o.getPartitionKeys().size());
      plan.loadOperator(o);
      PartitioningTestOperator partitionedInstance = (PartitioningTestOperator)plan.loadOperator(o);
      Assert.assertEquals("instance per partition", o.getPartitionKeys().values().toString(), partitionedInstance.pks);
    }

  }

}