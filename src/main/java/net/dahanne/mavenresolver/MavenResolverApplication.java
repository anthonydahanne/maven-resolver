package net.dahanne.mavenresolver;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Collections;

@SpringBootApplication
public class MavenResolverApplication {

  public static void main(String[] args) {
    SpringApplication.run(MavenResolverApplication.class, args);
  }


  @PostConstruct
  public void resolvePom() throws Exception {
    ContainerConfiguration config = new DefaultContainerConfiguration();
    config.setAutoWiring(true);
    config.setClassPathScanning(PlexusConstants.SCANNING_INDEX);
    PlexusContainer plexusContainer = new DefaultPlexusContainer(config);
    ProjectBuilder projectBuilder = plexusContainer.lookup(ProjectBuilder.class);
    RepositorySystem repositorySystem = plexusContainer.lookup(RepositorySystem.class);
    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
    LocalRepository localRepository = new LocalRepository("target/.m2");
    session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, localRepository));

    ArtifactRepository centralRepository = new MavenArtifactRepository();
    centralRepository.setUrl("https://repo.maven.apache.org/maven2/");
    centralRepository.setLayout(new DefaultRepositoryLayout());
    centralRepository.setSnapshotUpdatePolicy(new ArtifactRepositoryPolicy());
    centralRepository.setReleaseUpdatePolicy(new ArtifactRepositoryPolicy());
    ProjectBuildingRequest request = new DefaultProjectBuildingRequest()
      .setRepositorySession(session)
      .setResolveDependencies(true)
      .setRemoteRepositories(Collections.singletonList(centralRepository))
      .setSystemProperties(System.getProperties());

    try {
      ProjectBuildingResult result = projectBuilder.build(new File("pom.xml"), request);

      result.getDependencyResolutionResult().getDependencies().forEach(dependency ->
          System.out.println(
              "Provided pom depends on: " +
                  dependency.getArtifact().getGroupId() +
                  ":" + dependency.getArtifact().getGroupId() +
                  ":" + dependency.getArtifact().getVersion()
              )
      );
    } catch (ProjectBuildingException pbe) {
      for (ProjectBuildingResult pbr: pbe.getResults()) {
        for (ModelProblem problem: pbr.getProblems()) {
          problem.getException().printStackTrace();
        }
      }
    }

  }

}
