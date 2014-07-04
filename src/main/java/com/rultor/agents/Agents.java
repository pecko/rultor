/**
 * Copyright (c) 2009-2014, rultor.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the rultor.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.rultor.agents;

import com.jcabi.aspects.Immutable;
import com.jcabi.github.Github;
import com.jcabi.github.RtGithub;
import com.jcabi.http.wire.RetryWire;
import com.jcabi.manifests.Manifests;
import com.rultor.agents.daemons.EndsDaemon;
import com.rultor.agents.daemons.StartsDaemon;
import com.rultor.agents.github.EndsTalk;
import com.rultor.agents.github.GetsMergeRequest;
import com.rultor.agents.github.PostsMergeResult;
import com.rultor.agents.github.StartsTalk;
import com.rultor.agents.merge.EndsGitMerge;
import com.rultor.agents.merge.StartsGitMerge;
import com.rultor.agents.shells.RegistersShell;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Agents.
 *
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @since 1.0
 */
@Immutable
public final class Agents {

    /**
     * Create them.
     * @param config YAML config
     * @return List of them
     */
    public Collection<Agent> make(final String config) {
        final Collection<Agent> agents = new LinkedList<Agent>();
        final Github github = new RtGithub(
            new RtGithub(
                Manifests.read("Rultor-GithubToken")
            ).entry().through(RetryWire.class)
        );
        agents.addAll(
            Arrays.asList(
                new StartsTalk(github),
                new TalkAgent.Wrap(
                    new RegistersShell(
                        "build.rultor.com", 22,
                        "rultor-build",
                        Manifests.read("Rultor-SshPrivateKey")
                    )
                ),
                new TalkAgent.Wrap(
                    new GetsMergeRequest(
                        github,
                        Collections.singleton("yegor256")
                    )
                ),
                new TalkAgent.Wrap(
                    new StartsGitMerge(
                        "mvn help:system clean install -Pqulice --batch-mode --update-snapshots --errors --strict-checksums"
                    )
                ),
                new TalkAgent.Wrap(new StartsDaemon()),
                new TalkAgent.Wrap(new EndsDaemon()),
                new TalkAgent.Wrap(new EndsGitMerge()),
                new TalkAgent.Wrap(new PostsMergeResult(github)),
                new TalkAgent.Wrap(new EndsTalk(github))
            )
        );
        return agents;
    }

}