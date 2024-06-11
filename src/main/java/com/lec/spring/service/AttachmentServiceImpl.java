package com.lec.spring.service;

import com.lec.spring.domain.Attachment;
import com.lec.spring.repository.AttachmentRepository;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AttachmentServiceImpl implements AttachmentService {

    private AttachmentRepository repo;
    public AttachmentServiceImpl(SqlSession session){
        repo = session.getMapper(AttachmentRepository.class);
    }
    @Override
    public Attachment findById(Long id) {
        return repo.findById(id);
    }
}
