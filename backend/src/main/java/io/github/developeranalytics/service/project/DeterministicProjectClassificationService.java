package io.github.developeranalytics.service.project;

import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.domain.project.ProjectCategory;
import io.github.developeranalytics.domain.project.RepositoryProjectCategory;
import io.github.developeranalytics.domain.technology.RepositoryTechnologyEvidence;
import io.github.developeranalytics.domain.technology.TechnologyEvidenceType;
import io.github.developeranalytics.persistence.project.ProjectCategoryRepository;
import io.github.developeranalytics.persistence.project.RepositoryProjectCategoryRepository;
import io.github.developeranalytics.persistence.technology.RepositoryTechnologyEvidenceRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@ApplicationScoped
public class DeterministicProjectClassificationService {
    @Inject ProjectCategoryTaxonomyService taxonomyService;
    @Inject ProjectCategoryRepository categories;
    @Inject RepositoryProjectCategoryRepository classifications;
    @Inject RepositoryTechnologyEvidenceRepository technologyEvidence;

    @Transactional
    public Result classify(SourceRepository repository) {
        taxonomyService.seedBuiltInTaxonomyIfEmpty();
        List<RepositoryTechnologyEvidence> evidence=technologyEvidence.findForRepository(repository.getUser().getId(),repository.getId());
        Set<String> technologies=new HashSet<>();Set<String> files=new HashSet<>();
        for(RepositoryTechnologyEvidence item:evidence){technologies.add(item.getTechnology().getTechnologyKey());if((item.getEvidenceType()==TechnologyEvidenceType.FILE||item.getEvidenceType()==TechnologyEvidenceType.MANIFEST)&&item.getSourceValue()!=null)files.add(item.getSourceValue().toLowerCase(Locale.ROOT));}
        SignalContext context=new SignalContext(normalize(repository.getName()),normalize(repository.getDescription()),repository.getTopics().stream().map(this::normalize).filter(v->!v.isBlank()).collect(java.util.stream.Collectors.toSet()),technologies,files);
        Map<String,Score> scores=new LinkedHashMap<>();

        technology(scores,context,"react","web-application",5);technology(scores,context,"vite","web-application",2);
        technology(scores,context,"swift","mobile-application",4);technology(scores,context,"quarkus","backend-service",5);
        technology(scores,context,"spring-boot","backend-service",5);technology(scores,context,"docker","infrastructure-platform",2);
        technology(scores,context,"github-actions","devops-ci-cd",4);technology(scores,context,"kubernetes","infrastructure-platform",5);
        technology(scores,context,"terraform","infrastructure-platform",5);technology(scores,context,"postgresql","data-database",4);
        technology(scores,context,"markdown","documentation-education",4);

        topic(scores,context,"game","game",6);topic(scores,context,"ios","mobile-application",5);topic(scores,context,"android","mobile-application",5);topic(scores,context,"tvos","mobile-application",5);
        topic(scores,context,"api","api",5);topic(scores,context,"library","library",5);topic(scores,context,"framework","framework",5);topic(scores,context,"security","security",6);
        topic(scores,context,"observability","observability",6);topic(scores,context,"monitoring","observability",5);topic(scores,context,"automation","automation",5);topic(scores,context,"devops","devops-ci-cd",5);
        topic(scores,context,"kubernetes","infrastructure-platform",5);topic(scores,context,"terraform","infrastructure-platform",5);topic(scores,context,"machine-learning","ai-ml",6);topic(scores,context,"ai","ai-ml",5);
        topic(scores,context,"architecture","architecture-modelling",5);topic(scores,context,"documentation","documentation-education",5);topic(scores,context,"tutorial","documentation-education",5);
        topic(scores,context,"book","documentation-education",5);topic(scores,context,"writing","documentation-education",5);topic(scores,context,"novel","documentation-education",5);topic(scores,context,"manuscript","documentation-education",5);
        topic(scores,context,"prototype","experiment-prototype",5);topic(scores,context,"poc","experiment-prototype",5);

        file(scores,context,".github/workflows/","devops-ci-cd",4);file(scores,context,"dockerfile","infrastructure-platform",2);file(scores,context,"chart.yaml","infrastructure-platform",5);file(scores,context,"kustomization.yaml","infrastructure-platform",5);
        file(scores,context,"package.swift","mobile-application",3);file(scores,context,"pom.xml","backend-service",2);file(scores,context,"package.json","web-application",2);

        metadata(scores,context,"api","api",2);metadata(scores,context,"server","backend-service",2);metadata(scores,context,"backend","backend-service",2);metadata(scores,context,"library","library",2);
        metadata(scores,context,"sdk","library",2);metadata(scores,context,"framework","framework",2);metadata(scores,context,"tool","developer-tooling",2);metadata(scores,context,"cli","developer-tooling",2);
        metadata(scores,context,"automation","automation",2);metadata(scores,context,"security","security",2);metadata(scores,context,"integration","integration",2);metadata(scores,context,"connector","integration",2);
        metadata(scores,context,"architecture","architecture-modelling",2);metadata(scores,context,"docs","documentation-education",3);metadata(scores,context,"documentation","documentation-education",3);
        metadata(scores,context,"book","documentation-education",4);metadata(scores,context,"writing","documentation-education",4);metadata(scores,context,"novel","documentation-education",4);metadata(scores,context,"manuscript","documentation-education",4);
        metadata(scores,context,"prototype","experiment-prototype",2);metadata(scores,context,"poc","experiment-prototype",2);metadata(scores,context,"game","game",3);

        classifications.deleteDeterministicForRepository(repository.getId());
        OffsetDateTime observedAt=OffsetDateTime.now(ZoneOffset.UTC);int assignments=0;
        for(Map.Entry<String,Score> entry:scores.entrySet()){
            Score score=entry.getValue();if(score.points<3)continue;
            ProjectCategory category=categories.findByKey(entry.getKey()).orElseThrow(()->new IllegalStateException("Project category missing: "+entry.getKey()));
            RepositoryProjectCategory.Confidence confidence=score.points>=8?RepositoryProjectCategory.Confidence.HIGH:score.points>=5?RepositoryProjectCategory.Confidence.MEDIUM:RepositoryProjectCategory.Confidence.LOW;
            Map<String,Object> rationale=new LinkedHashMap<>();rationale.put("score",score.points);rationale.put("signals",List.copyOf(score.signals));rationale.put("method","deterministic");
            classifications.persist(new RepositoryProjectCategory(repository,category,RepositoryProjectCategory.Source.DETERMINISTIC,confidence,rationale,observedAt));assignments++;
        }
        return new Result(assignments,scores.size());
    }

    private void technology(Map<String,Score>s,SignalContext c,String technology,String category,int points){if(c.technologies.contains(technology))add(s,category,points,"technology:"+technology);}
    private void topic(Map<String,Score>s,SignalContext c,String topic,String category,int points){if(c.topics.contains(topic))add(s,category,points,"topic:"+topic);}
    private void file(Map<String,Score>s,SignalContext c,String fragment,String category,int points){String n=normalize(fragment);if(c.files.stream().anyMatch(path->path.contains(n)))add(s,category,points,"file:"+fragment);}
    private void metadata(Map<String,Score>s,SignalContext c,String token,String category,int points){String n=normalize(token);if(c.name.contains(n)||c.description.contains(n))add(s,category,points,"metadata:"+token);}
    private void add(Map<String,Score>scores,String category,int points,String signal){Score score=scores.computeIfAbsent(category,x->new Score());score.points+=points;score.signals.add(signal);}
    private String normalize(String value){return value==null?"":value.trim().toLowerCase(Locale.ROOT);}
    private record SignalContext(String name,String description,Set<String> topics,Set<String> technologies,Set<String> files){}
    private static class Score{int points;final List<String> signals=new ArrayList<>();}
    public record Result(int categoriesAssigned,int candidateCategories){}
}
