/*
 * (c) Copyright 2008 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.tdb.store;

import iterator.Iter;
import iterator.Transform;

import java.util.Iterator;

import lib.Tuple;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.shared.LockMRSW;

import com.hp.hpl.jena.sparql.core.Closeable;
import com.hp.hpl.jena.sparql.core.DatasetGraph;

import com.hp.hpl.jena.tdb.base.file.Location;
import com.hp.hpl.jena.tdb.lib.NodeLib;
import com.hp.hpl.jena.tdb.lib.Sync;
import com.hp.hpl.jena.tdb.solver.reorder.ReorderTransformation;

public class DatasetGraphTDB implements DatasetGraph, Sync, Closeable
{
    private final TripleTable tripleTable ;
    private final GraphTriplesTDB defaultGraph ;
    private final QuadTable quadTable ;
    private final Lock lock = new LockMRSW() ;
    private final ReorderTransformation transform ;

    public DatasetGraphTDB(TripleTable tripleTable, QuadTable quadTable, ReorderTransformation transform, Location location)
    {
        this.tripleTable = tripleTable ;
        this.quadTable = quadTable ;
        this.transform = transform ;
        defaultGraph = new GraphTriplesTDB(tripleTable, transform, location) ;
    }
    
    public QuadTable getQuadTable()                 { return quadTable ; }
    public TripleTable getDefaultTripleTableTable() { return tripleTable ; } 
    
    @Override
    public boolean containsGraph(Node graphNode)
    {
        return false ;
    }

    @Override
    public Graph getDefaultGraph()
    {
        return defaultGraph ;
    }

    @Override
    public Graph getGraph(Node graphNode)
    {
        return new GraphNamedTDB(this, graphNode, transform);
    }

    @Override
    public Lock getLock()   { return lock ; }

    @Override
    public Iterator<Node> listGraphNodes()
    {
        Iterator<Tuple<NodeId>> x = quadTable.getTupleTable().getIndex(0).all() ;
        Transform<Tuple<NodeId>, NodeId> project = new Transform<Tuple<NodeId>, NodeId>()
        {
            @Override
            public NodeId convert(Tuple<NodeId> item)
            {
                return item.get(0) ;
            }
        } ;
        Iterator<NodeId> z =  Iter.iter(x).map(project).distinct() ;
        return NodeLib.nodes(quadTable.getNodeTable(), z) ;
    }

    @Override
    public int size()                   { return -1 ; }
    
    public Location getLocation()       { return defaultGraph.getLocation() ; }

    @Override
    public void sync(boolean force)
    {
        tripleTable.sync(force) ;
        quadTable.sync(force) ;
    }

    @Override
    public void close()
    {
        tripleTable.close() ;
        quadTable.close() ;
    }
}

/*
 * (c) Copyright 2008 Hewlett-Packard Development Company, LP
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */