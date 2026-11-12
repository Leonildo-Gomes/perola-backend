package cv.perola.core.storage;

import org.springframework.core.io.Resource;

import java.io.InputStream;

/**
 * Porta de abstração de armazenamento de ficheiros de media fora da base de dados.
 * Permite gravação em pasta local / Docker Volume no MVP e migração transparente
 * para Object Storage (AWS S3, Cloudflare R2, MinIO) em produção.
 */
public interface FileStoragePort {

    /**
     * Armazena o conteúdo binário sob a chave relativa especificada.
     *
     * @param inputStream fluxo de bytes do ficheiro
     * @param storageKey  caminho relativo único (ex: businesses/{id}/services/{uuid}.webp)
     * @param contentType MIME type validado (ex: image/webp, image/jpeg, image/png)
     */
    void store(InputStream inputStream, String storageKey, String contentType);

    /**
     * Carrega um ficheiro armazenado como {@link Resource} do Spring.
     *
     * @param storageKey caminho relativo único do ficheiro
     * @return recurso legível
     */
    Resource loadAsResource(String storageKey);

    /**
     * Remove um ficheiro armazenado caso exista.
     *
     * @param storageKey caminho relativo único do ficheiro
     */
    void delete(String storageKey);
}

